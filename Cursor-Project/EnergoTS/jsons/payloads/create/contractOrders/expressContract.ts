import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type BankingDetails = {
    directDebit: boolean;
    bankId: string | number | null;
    bic: string | null;
    iban: string | null;
};

type ExpressContractParameters = {
    productId: string | number | null;
    productVersionId: number;
    procurementLaw: boolean;
    signingDate: string | null;
    campaignId: string | number | null;
    estimatedTotalConsumption: string;
    bankingDetails: BankingDetails;
};

type LocalAddressData = {
    countryId: string | number;
    regionId: string | number;
    municipalityId: string | number;
    populatedPlaceId: string | number;
    zipCodeId: string | number;
    districtId: string | number | null;
    residentialAreaId: string | number | null;
    streetId: string | number | null;
    streetType: string | null;
    residentialAreaType: string | null;
};

type Address = {
    foreign: boolean;
    localAddressData: LocalAddressData;
    number: string | null;
    additionalInformation: string | null;
    block: string | null;
    entrance: string | null;
    floor: string | null;
    apartment: string | null;
    mailbox: string | null;
};

type ContactRequest = {
    contactType: string;
    contactValue: string;
};

type Communication = {
    id: string | number | null;
    address: Address;
    contactRequests: ContactRequest[];
    communicationTypes: string;
};

type BusinessCustomerDetails = {
    name: string;
    nameTranslated: string;
    procurementLaw: boolean;
    legalFormId: string | number;
    legalFormTransId: string | number;
};

type ManagerRequest = {
    id: string | number | null;
    titleId: string | number;
    name: string;
    middleName: string | null;
    surname: string;
    jobPosition: string;
    personalNumber: string | null;
    representationMethodId: string | number;
    unrecognizedIdentifier: boolean;
};

type Customer = {
    id: string | number | null;
    detailId: string | number | null;
    identifier: string;
    customerType: string;
    foreign: boolean;
    consentToMarketingCommunication: boolean;
    preferCommunicationInEnglish: boolean;
    vatNumber: string | null;
    ownershipFormId: string | number;
    economicBranchCiId: string | number;
    mainActivitySubject: string;
    businessCustomerDetails: BusinessCustomerDetails;
    businessActivityName: string;
    businessActivityNameTransl: string;
    customerSegments: (string | number)[];
    communications: Communication[];
    address: Address;
    managerRequests: ManagerRequest[];
};

type ProductParameters = {
    contractType: string;
    productContractTermId: string | number | null;
    paymentGuarantee: string;
    cashDeposit: number | null;
    bankGuarantee: number | null;
    contractFormulas: any[];
    invoicePaymentTermId: string | number | null;
    invoicePaymentTermValue: number;
    entryIntoForce: string;
    startOfContractInitialTerm: string;
    supplyActivation: string;
    productContractWaitForOldContractTermToExpires: string;
    monthlyInstallmentValue: number | null;
    monthlyInstallmentAmount: number | null;
    interimAdvancePayments: any[];
    productAdditionalParams: any[];
};

type ExpressContract = {
    podDetailIds: (string | number | null)[];
    expressContractParameters: ExpressContractParameters;
    expressContractType: string;
    customer: Customer;
    proxyRequest: any[];
    isCheckboxSelected: boolean;
    productParameters: ProductParameters;
};

export function expressContract(): ExpressContract {
    return {
    
    "podDetailIds": [],
    "expressContractParameters": {
        "productId": null, // from product create
        "productVersionId": 1,
        "procurementLaw": false,
        "signingDate": null,
        "campaignId": null,
        "estimatedTotalConsumption": "0.012",
        "bankingDetails": {
            "directDebit": false,
            "bankId": null,
            "bic": null,
            "iban": null
        }
    },
    "expressContractType": "PRODUCT",
    "customer": {
        "id": null,
        "detailId": null,
        "identifier": randomGens.generateRandomString(false, true, 10),
        "customerType": "LEGAL_ENTITY",
        "foreign": true,
        "consentToMarketingCommunication": false,
        "preferCommunicationInEnglish": false,
        "vatNumber": null,
        "ownershipFormId": envVariables.form_of_ownership,
        "economicBranchCiId": envVariables.economic_branch_based_on_commercial_information,
        "mainActivitySubject": "AUTOMATION",
        "businessCustomerDetails": {
            "name": "AUTOMATION",
            "nameTranslated": "AUTOMATION",
            "procurementLaw": false,
            "legalFormId": envVariables.legal_forms,
            "legalFormTransId": envVariables.legal_forms
        },
        "businessActivityName": "ДЕТСКИ ГРАДИНИ",
        "businessActivityNameTransl": "KINDERGARTEN",
        "customerSegments": [
            envVariables.segment
        ],
     "communications": [
            {
                "id": null,
                "address": {
                    "foreign": false,
                    "localAddressData": {
                        "countryId": envVariables.countries,
                        "regionId": envVariables.regions,
                        "municipalityId": envVariables.municipalities,
                        "populatedPlaceId": envVariables.population_places,
                        "zipCodeId": envVariables.zip_codes,
                        "districtId": null,
                        "residentialAreaId": null,
                        "streetId": null,
                        "streetType": null,
                        "residentialAreaType": null
                    },
                    "number": null,
                    "additionalInformation": null,
                    "block": null,
                    "entrance": null,
                    "floor": null,
                    "apartment": null,
                    "mailbox": null
                },
                "contactRequests": [
                    {
                        "contactType": "EMAIL",
                        "contactValue": randomGens.generateRandomEMail()
                    },
                    {
                        "contactType": "MOBILE_NUMBER",
                        "contactValue": "61256321512"
                    }
                ],
                "communicationTypes": "CONTRACT_COMMUNICATION"
            },
            {
                "id": null,
                "address": {
                    "foreign": false,
                    "localAddressData": {
                       "countryId": envVariables.countries,
                        "regionId": envVariables.regions,
                        "municipalityId": envVariables.municipalities,
                        "populatedPlaceId": envVariables.population_places,
                        "zipCodeId": envVariables.zip_codes,
                        "districtId": null,
                        "residentialAreaId": null,
                        "streetId": null,
                        "streetType": null,
                        "residentialAreaType": null
                    },
                    "number": null,
                    "additionalInformation": null,
                    "block": null,
                    "entrance": null,
                    "floor": null,
                    "apartment": null,
                    "mailbox": null
                },
                "contactRequests": [
                    {
                        "contactType": "EMAIL",
                        "contactValue": randomGens.generateRandomEMail()
                    },
                    {
                        "contactType": "MOBILE_NUMBER",
                        "contactValue": "24124124124"
                    }
                ],
                "communicationTypes": "INVOICE_ISSUANCE"
            }
        ],
        "address": {
            "foreign": false,
            "localAddressData": {
               "countryId": envVariables.countries,
                "regionId": envVariables.regions,
                "municipalityId": envVariables.municipalities,
                "populatedPlaceId": envVariables.population_places,
                "zipCodeId": envVariables.zip_codes,
                "districtId": null,
                "residentialAreaId": null,
                "streetId": null,
                "streetType": null,
                "residentialAreaType": null
            },
            "number": null,
            "additionalInformation": null,
            "block": null,
            "entrance": null,
            "floor": null,
            "apartment": null,
            "mailbox": null
        },
        "managerRequests": [
            {
                "id": null,
                "titleId": envVariables.title,
                "name": "ASF",
                "middleName": null,
                "surname": "ASF",
                "jobPosition": "ASF",
                "personalNumber": null,
                "representationMethodId": envVariables.method_of_representation,
                "unrecognizedIdentifier": true
            }
        ]
    },
    "proxyRequest": [],
    "isCheckboxSelected": false,
    "productParameters": {
        "contractType": "COMBINED",
        "productContractTermId": null, // from product
        "paymentGuarantee": "NO",
        "cashDeposit": null,
        "bankGuarantee": null,
        "contractFormulas": [],
        "invoicePaymentTermId": null, //from term
        "invoicePaymentTermValue": 10,
        "entryIntoForce": "SIGNING",
        "startOfContractInitialTerm": "SIGNING",
        "supplyActivation": "FIRST_DAY_OF_MONTH",
        "productContractWaitForOldContractTermToExpires": "NO",
        "monthlyInstallmentValue": null,
        "monthlyInstallmentAmount": null,
        "interimAdvancePayments": [],
        "productAdditionalParams": []
    }
    };
}