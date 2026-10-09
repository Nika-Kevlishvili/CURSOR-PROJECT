import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type ContractStatus = 'SIGNED' | 'NEW' | 'TERMINATED' | string;
type SubStatus = 'SIGNED_BY_BOTH_SIDES' | string;
type VersionStatus = 'SIGNED' | 'DRAFT' | string;
type ContractType = 'WITHOUT_SUPPLY' | 'SUPPLY_ONLY' | 'COMBINED' | 'SUPPLY_BALANCING' | string;
type PaymentGuarantee = 'NO' | 'CASH_DEPOSIT' | 'BANK' | 'CASH_DEPOSIT_AND_BANK' | string;
type EntryMode = 'MANUAL' | 'AUTOMATIC' | string;
type SupplyActivation = 'MANUAL' | 'AUTOMATIC' | string;

type BasicParameters = {
    status: ContractStatus;
    subStatus: SubStatus;
    versionStatus: VersionStatus;
    statusModifyDate: string; // yyyy-mm-dd
    productId: number | null;
    contractNumber: string | number | null;
    creationDate: string | null;
    type: 'CONTRACT' | string;
    hasUntilAmount: boolean;
    hasUntilVolume: boolean;
    versionTypeIds: (number | string)[];
    procurementLaw: boolean;
    untilAmount: number | null;
    untilVolume: number | null;
    untilAmountCurrencyId: number | null;
    signingDate: string | null; // could be fixed literal in template
    entryInForceDate: string | null;
    startOfInitialTerm: string | null;
    contractTermEndDate: string | null;
    activationDate: string | null;
    perpetuityDate: string | null;
    terminationDate: string | null;
    customerId: number | null;
    customerVersionId: number | null;
    communicationDataBillingId: number | null;
    communicationDataContractId: number | null;
    relatedEntities: any[];
    files: any[];
    documents: any[];
    productVersionId: number | null;
    proxy: any; // unknown shape currently
}

type BankingDetails = {
    directDebit: boolean;
    bankId: number | null;
    iban: string | null;
}

type AdditionalParameters = {
    dealNumber: string | number | null;
    estimatedTotalConsumptionUnderContractKwh: string | number | null;
    bankingDetails: BankingDetails;
    riskAssessmentAdditionalConditions: any[];
    interestRateId: number | string | null;
    campaignId: number | string | null;
    internalIntermediaries: any[];
    externalIntermediaries: any[];
    assistingEmployees: any[];
    riskAssessment?: number | string | null;
    employeeId?: number | string | null;
}

type ProductParameters = {
    contractType: ContractType;
    productContractTermId: number | null;
    contractTermEndDate: string | null;
    paymentGuarantee: PaymentGuarantee;
    cashDeposit: number | null;
    bankGuarantee: number | null;
    guaranteeInformation: string | null;
    guaranteeContract: boolean;
    contractFormulas: any[];
    productAdditionalParams: any[];
    invoicePaymentTermId: number | null;
    invoicePaymentTermValue: number | null;
    entryIntoForce: EntryMode;
    entryIntoForceValue: number | string | null;
    startOfContractInitialTerm: EntryMode;
    startOfContractValue: number | string | null;
    firstDayOfMonthValue: number | null;
    supplyActivation: SupplyActivation;
    supplyActivationValue: number | string | null;
    interimAdvancePayments: any[];
    monthlyInstallmentValue: number | null;
    monthlyInstallmentAmount: number | null;
    marginalPrice: number | null;
    marginalPriceValidity: string | null;
    hourlyLoadProfile: string | null;
    procurementPrice: number | null;
    imbalancePriceIncrease: number | null;
    setMargin: number | null;
    productContractWaitForOldContractTermToExpires: boolean | null;
}

type ProductContractPointOfDelivery = {
    pointOfDeliveryDetailId: number | null;
    dealNumber: string | number | null;
}

export type ProductContractPayload = {
    basicParameters: BasicParameters;
    additionalParameters: AdditionalParameters;
    productParameters: ProductParameters;
    proxy: Record<string, any>;
    podDetailIds: Record<string, any>;
    productContractPointOfDeliveries: ProductContractPointOfDelivery[];
}

export function ProductContract(): ProductContractPayload {
    return {
        "basicParameters": {
            "status": "SIGNED",
            "subStatus": "SIGNED_BY_BOTH_SIDES",
            "versionStatus": "SIGNED",
            "statusModifyDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
            "productId": null,
            "contractNumber": null,
            "creationDate": null,
            "type": "CONTRACT",
            "hasUntilAmount": false,
            "hasUntilVolume": false,
            "versionTypeIds": [
                envVariables.contract_version_types
            ],
            "procurementLaw": false,
            "untilAmount": null,
            "untilVolume": null,
            "untilAmountCurrencyId": null,
            "signingDate": "2025-01-01",
            "entryInForceDate": null,
            "startOfInitialTerm": null,
            "contractTermEndDate": null,
            "activationDate": null,
            "perpetuityDate": null,
            "terminationDate": null,
            "customerId": null,
            "customerVersionId": 1,
            "communicationDataBillingId": null,
            "communicationDataContractId": null,
            "relatedEntities": [],
            "files": [],
            "documents": [],
            "productVersionId": null,
            "proxy": null
        },
        "additionalParameters": {
            "dealNumber": null,
            "estimatedTotalConsumptionUnderContractKwh": null,
            "bankingDetails": {
                "directDebit": false,
                "bankId": null,
                "iban": null
            },
            "riskAssessmentAdditionalConditions": [],
            "interestRateId": envVariables.interest_rate,
            "campaignId": envVariables.campaign,
            "internalIntermediaries": [],
            "externalIntermediaries": [],
            "assistingEmployees": []
        },
        "productParameters": {
            "contractType": "WITHOUT_SUPPLY",
            "productContractTermId": null,
            "contractTermEndDate": null,
            "paymentGuarantee": "NO",
            "cashDeposit": null,
            "bankGuarantee": null,
            "guaranteeInformation": null,
            "guaranteeContract": false,
            "contractFormulas": [],
            "productAdditionalParams": [],
            "invoicePaymentTermId": null,
            "invoicePaymentTermValue": 10,
            "entryIntoForce": "MANUAL",
            "entryIntoForceValue": null,
            "startOfContractInitialTerm": "MANUAL",
            "startOfContractValue": null,
            "firstDayOfMonthValue": null,
            "supplyActivation": "MANUAL",
            "supplyActivationValue": null,
            "interimAdvancePayments": [],
            "monthlyInstallmentValue": null,
            "monthlyInstallmentAmount": null,
            "marginalPrice": null,
            "marginalPriceValidity": null,
            "hourlyLoadProfile": null,
            "procurementPrice": null,
            "imbalancePriceIncrease": null,
            "setMargin": null,
            "productContractWaitForOldContractTermToExpires": null
        },
        "proxy": {},
        "podDetailIds": {},
        "productContractPointOfDeliveries": []
    }
}