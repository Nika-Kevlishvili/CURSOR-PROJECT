import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

// -------------------- Type Definitions --------------------
type ContractType = 'COMBINED' | 'SUPPLY_BALANCING' | 'SUPPLY_ONLY' | 'WITHOUT_SUPPLY';
type PaymentGuarantee = 'NO' | 'CASH_DEPOSIT' | 'BANK' | 'CASH_DEPOSIT_AND_BANK';
type TypePointOfDelivery = 'CONSUMER' | 'GENERATOR';
type PurposeOfConsumption = 'HOUSEHOLD' | 'NON_HOUSEHOLD';
type MeteringType = 'SETTLEMENT_PERIOD' | 'SLP';
type VoltageLevel = 'LOW' | 'MEDIUM' | 'MEDIUM_DIRECT_CONNECTED' | 'HIGH';
type ProductStatus = 'ACTIVE' | 'INACTIVE' | 'NEW';

type TemplateType = 'EMAIL_TEMPLATE' | 'INVOICE_TEMPLATE' | 'CONTRACT_TEMPLATE';

type ProductTerm = {
    typeOfTerms: string; // e.g. 'PERIOD'
    value: string;       // numeric string per current payload
    periodType: string | null; // e.g. 'DAY_DAYS'
    renewalPeriodValue: string | null;
    renewalPeriodType: string | null;
    perpetuityCause: boolean;
    automaticRenewal: boolean | null;
    numberOfRenewals: number | null;
    name: string;
    id: number | null;
}

type ProductTemplateRef = {
    templateId: number;
    templateType: TemplateType;
}

type SpecialOfferCustomer = {
    customerId: number | null;
    customerDetailId: number | null;
    name: string;
    versionId: number;
    versionName: string;
    displayDate: string;
}

type SpecialOfferContract = {
    contractId: number | null;
    contractDetailId: number | null;
    name: string;
    versionId: number;
    displayDate: string;
}

type SpecialOfferResigningProduct = {
    productId: number | null;
    versionIds: number[];
}

export type SpecialOffersTabPayload = {
    promotionalProduct: boolean;
    applyToSpecificCustomers: boolean;
    preferenceIds: number[];
    customers: SpecialOfferCustomer[];
    contracts: SpecialOfferContract[];
    resignable: boolean;
    resigningProducts: SpecialOfferResigningProduct[];
}

export type ProductPayload = {
    isIndividual: boolean;
    customerIdentifier: string | number | null;
    name: string;
    productDetailIdsForUpdatingProductContracts: (number | string)[];
    nameTransliterated: string;
    printingName: string;
    preferenceIds: (number | string)[];
    printingNameTransliterated: string;
    productStatus: ProductStatus;
    consumerBalancingProductNameId: number | null;
    generatorBalancingProductNameId: number | null;
    availableForSale: boolean | null;
    availableFrom: string | null;
    availableTo: string | null;
    shortDescription: string | null;
    fullDescription: string | null;
    productGroupId: number;
    otherSystemConnectionCode: string | null;
    productTypeId: number;
    contractTypes: ContractType[];
    paymentGuarantees: PaymentGuarantee[];
    typePointsOfDelivery: TypePointOfDelivery[];
    purposeOfConsumptions: PurposeOfConsumption[];
    meteringTypeOfThePointOfDeliveries: MeteringType[];
    voltageLevels: VoltageLevel[];
    gridOperatorIds: (number | string)[];
    cashDepositAmount: number | null;
    cashDepositCurrencyId: number | null;
    bankGuaranteeAmount: number | null;
    bankGuaranteeCurrencyId: number | null;
    invoiceAndTemplatesText: string | null;
    invoiceAndTemplatesTextTransliterated: string | null;
    incomeAccountNumber: string;
    costCenterControllingOrder: string;
    globalVatRate: boolean;
    vatRateId: number | null;
    globalSalesChannel: boolean;
    salesChannelIds: number[] | null;
    globalSalesArea: boolean;
    salesAreasIds: number[] | null;
    globalGridOperator: boolean;
    globalSegment: boolean;
    segmentIds: number[] | null;
    electricityPriceTypeId: number;
    equalMonthlyInstallmentsActivation: boolean;
    installmentNumber: number | null;
    installmentNumberFrom: number | null;
    installmentNumberTo: number | null;
    amount: number | null;
    amountFrom: number | null;
    amountTo: number | null;
    currencyId: number | null;
    additionalInfo1: string | null;
    additionalInfo2: string | null;
    additionalInfo3: string | null;
    additionalInfo4: string | null;
    additionalInfo5: string | null;
    additionalInfo6: string | null;
    additionalInfo7: string | null;
    additionalInfo8: string | null;
    additionalInfo9: string | null;
    additionalInfo10: string | null;
    scheduleRegistrations: any[]; // refine if structure known
    forecasting: any[];            // refine if structure known
    takingOverBalancingCosts: any[]; // refine if structure known
    capacityLimitType: string | null;
    capacityLimitAmount: number | null;
    collectionChannelIds: number[] | null;
    productTerms: ProductTerm[];
    productAdditionalParams: any[]; // refine later
    termId: number | null;
    termGroupId: number | null;
    interimAdvancePayments: any[]; // refine later
    interimAdvancePaymentGroups: any[]; // refine later
    priceComponentIds: (number | null)[];
    priceComponentGroupIds: number[];
    terminationIds: (number | null)[];
    terminationGroupIds: number[];
    penaltyIds: (number | null)[];
    penaltyGroupIds: number[];
    relatedEntities: any[];
    productFileIds: (number | string)[];
    templateIds: ProductTemplateRef[];
}

export function product_create(): ProductPayload {
    return {
        "isIndividual": false,
        "customerIdentifier": null,
        "name": `product${randomGens.generateRandomString(true,false,10)}`,
        "productDetailIdsForUpdatingProductContracts": [],
        "nameTransliterated": randomGens.generateRandomString(true,false,10),
        "printingName": randomGens.generateRandomString(true,false,10),
        "printingNameTransliterated": randomGens.generateRandomString(true,false,10),
        "productStatus": "ACTIVE",
        "consumerBalancingProductNameId": null,
        "generatorBalancingProductNameId": null,
        "availableForSale": true,
        "availableFrom": null,
        "availableTo": null,
        "shortDescription": "Description for automation product",
        "fullDescription": null,
        "productGroupId": envVariables.product_group,
        "otherSystemConnectionCode": null,
        "productTypeId": envVariables.product_type,
        "contractTypes": [
            "COMBINED",
            "SUPPLY_BALANCING",
            "SUPPLY_ONLY",
            "WITHOUT_SUPPLY"
        ],
        "paymentGuarantees": [
            "NO",
            "CASH_DEPOSIT",
            "BANK",
            "CASH_DEPOSIT_AND_BANK"
        ],
        "typePointsOfDelivery": [
            "CONSUMER",
            "GENERATOR"
        ],
        "purposeOfConsumptions": [
            "HOUSEHOLD",
            "NON_HOUSEHOLD"
        ],
        "meteringTypeOfThePointOfDeliveries": [
            "SETTLEMENT_PERIOD",
            "SLP"
        ],
        "voltageLevels": [
            "LOW",
            "MEDIUM",
            "MEDIUM_DIRECT_CONNECTED",
            "HIGH"
        ],
        "gridOperatorIds": [],
        "cashDepositAmount": null,
        "cashDepositCurrencyId": null,
        "bankGuaranteeAmount": null,
        "bankGuaranteeCurrencyId": null,
        "invoiceAndTemplatesText": null,
        "invoiceAndTemplatesTextTransliterated": null,
        "incomeAccountNumber": "2",
        "costCenterControllingOrder": "2",
        "globalVatRate": true,
        "preferenceIds": [envVariables.preferences],
        "vatRateId": null,
        "globalSalesChannel": true,
        "salesChannelIds": null,
        "globalSalesArea": true,
        "salesAreasIds": null,
        "globalGridOperator": true,
        "globalSegment": true,
        "segmentIds": null,
        "electricityPriceTypeId": envVariables.price_type_for_electricity,
        "equalMonthlyInstallmentsActivation": false,
        "installmentNumber": null,
        "installmentNumberFrom": null,
        "installmentNumberTo": null,
        "amount": null,
        "amountFrom": null,
        "amountTo": null,
        "currencyId": null,
        "additionalInfo1": null,
        "additionalInfo2": null,
        "additionalInfo3": null,
        "additionalInfo4": null,
        "additionalInfo5": null,
        "additionalInfo6": null,
        "additionalInfo7": null,
        "additionalInfo8": null,
        "additionalInfo9": null,
        "additionalInfo10": null,
        "scheduleRegistrations": [],
        "forecasting": [],
        "takingOverBalancingCosts": [],
        "capacityLimitType": null,
        "capacityLimitAmount": null,
        "collectionChannelIds": null,
        "productTerms": [
            {
                "typeOfTerms": "PERIOD",
                "value": "100",
                "periodType": "DAY_DAYS",
                "renewalPeriodValue": null,
                "renewalPeriodType": null,
                "perpetuityCause": false,
                "automaticRenewal": null,
                "numberOfRenewals": null,
                "name": "100 Day/Days Period ",
                "id": null
            }
        ],
        "productAdditionalParams": [],
        "termId": null,
        "termGroupId": null,
        "interimAdvancePayments": [],
        "interimAdvancePaymentGroups": [],
    "priceComponentIds": [],
    "priceComponentGroupIds": [],
    "terminationIds": [],
    "terminationGroupIds": [],
    "penaltyIds": [],
    "penaltyGroupIds": [],
        "relatedEntities": [],
        "productFileIds": [],
        "templateIds": [
            {
                "templateId": envVariables.invoice_email_template,
                "templateType": "EMAIL_TEMPLATE"
            },
            {
                "templateId": envVariables.invoice_document_template,
                "templateType": "INVOICE_TEMPLATE"
            },
            {
                "templateId": envVariables.product_contract_template,
                "templateType": "CONTRACT_TEMPLATE"
            }
        ]
    }
}

export function specialOffersTab_create(): SpecialOffersTabPayload {
    return {
        "promotionalProduct": true,
        "applyToSpecificCustomers": true,
        "preferenceIds": [
            envVariables.preferences
        ],
        "customers": [
            {
                "customerId": null,
                "customerDetailId": null,
                "name": "automation (playwright)",
                "versionId": 1,
                "versionName": "automation (playwright)",
                "displayDate": "automation (playwright)"
            }
        ],
        "contracts": [
            {
                "contractId": null,
                "contractDetailId": null,
                "name": "automation (playwright)",
                "versionId": 1,
                "displayDate": "automation (playwright)"
            }
        ],
        "resignable": false,
        "resigningProducts": []
    }
}