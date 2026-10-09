import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type Status = 'ACTIVE' | 'INACTIVE' | 'NEW';

type PrivateCustomerDetails = {
    gdprRegulationConsent: boolean;
    firstName: string;
    firstNameTranslated: string;
    middleName: string | null;
    middleNameTranslated: string | null;
    lastName: string;
    lastNameTranslated: string;
}

type LocalAddressData = {
    countryId: number;
    regionId: number;
    municipalityId: number;
    populatedPlaceId: number;
    zipCodeId: number;
    districtId: number;
    residentialAreaId: number;
    streetId: number;
    streetType: string;
    residentialAreaType: string;
}

type Address = {
    foreign: boolean;
    localAddressData: LocalAddressData;
    number: string | null;
    additionalInformation: string | null;
    block: string | number | null;
    entrance: string | null;
    floor: string | null;
    apartment: string | null;
    mailbox: string | null;
}

type AddressTransl = {
    foreign: boolean;
    localAddressData: {
        country: string;
        region: string;
        municipality: string;
        populatedPlace: string;
        zipCode: string;
        district: string;
        residentialArea: string;
        street: string;
    };
    number: string | null;
    additionalInformation: string | null;
    block: string | null;
    entrance: string | null;
    floor: string | null;
    apartment: string | null;
    mailbox: string | null;
}

type BankingDetails = {
    directDebit: boolean;
    bankId: string | number;
    bic: string | null;
    iban: string | null;
    declaredConsumption: number | null;
    preferenceIds: Array<string | number>;
    creditRatingId: number;
}

type CommunicationPurpose = {
    contactPurposeId: string | number;
    status: Status;
}

type CommunicationAddress = Omit<Address, 'localAddressData'> & {
    localAddressData: Omit<LocalAddressData, 'districtId' | 'residentialAreaId' | 'streetId' | 'streetType' | 'residentialAreaType'> & {
        districtId: number;
        residentialAreaId: number;
        streetId: number;
        streetType: string;
        residentialAreaType: string;
    };
};

type CommunicationContact = {
    sendSms: boolean;
    platformId: number | null;
    status: Status;
    contactType: 'MOBILE_NUMBER' | 'LANDLINE_PHONE' | 'CALL_CENTER' | 'FAX' | 'EMAIL' | 'WEBSITE' | 'OTHER_PLATFORM' | string;
    contactValue: string;
}

type CommunicationDataEntry = {
    status: Status;
    contactTypeName: string;
    contactPurposes: CommunicationPurpose[];
    address: CommunicationAddress;
    communicationContacts: CommunicationContact[];
    contactPersons: unknown[];
}

type CustomerPrivatePayload = {
    customerType: 'PRIVATE_CUSTOMER';
    customerIdentifier: string;
    foreign: boolean;
    marketingConsent: boolean;
    oldCustomerNumber: string | null;
    customerDetailStatus: Status;
    businessActivity: boolean;
    preferCommunicationInEnglish: boolean;
    privateCustomerDetails: PrivateCustomerDetails;
    segmentIds: number[];
    address: Address;
    addressTransl: AddressTransl;
    bankingDetails: BankingDetails;
    relatedCustomers: unknown;
    communicationData: CommunicationDataEntry[];
    accountManagers: unknown[];
    customerAdditionalInformation: unknown;
    customerEditContractRequests: unknown[];
}

export function customerPrivate(): CustomerPrivatePayload {
    return {
    "customerType": "PRIVATE_CUSTOMER",
    "customerIdentifier": '',
    "foreign": false,
    "marketingConsent": false,
    "oldCustomerNumber": null,
    "customerDetailStatus": "NEW",
    "businessActivity": false,
    "preferCommunicationInEnglish": false,
    "privateCustomerDetails": {
        "gdprRegulationConsent": false,
        "firstName": "NAME",
        "firstNameTranslated": "NAME",
    "middleName": "MIDDLE NAME",
    "middleNameTranslated": "MIDDLE NAME",
        "lastName": "LAST NAME",
        "lastNameTranslated": "LAST NAME"
    },
    "segmentIds": [
        envVariables.segment
    ],
    "address": {
        "foreign": false,
        "localAddressData": {
            "countryId": envVariables.countries,
            "regionId": envVariables.regions,
            "municipalityId": envVariables.municipalities,
            "populatedPlaceId": envVariables.population_places,
            "zipCodeId": envVariables.zip_codes,
            "districtId": envVariables.districts,
            "residentialAreaId": envVariables.residential_areas,
            "streetId": envVariables.streets,
            "streetType": "STREET",
            "residentialAreaType": "QUARTER"
        },
        "number": "122",
        "additionalInformation": null,
        "block": null,
        "entrance": null,
        "floor": null,
        "apartment": null,
        "mailbox": null
    },
    "addressTransl": {
        "foreign": false,
        "localAddressData": {
            "country": "STANDARD COUNTRY",
            "region": "STANDARD REGION",
            "municipality": "STANDARD MUNICIPALITY",
            "populatedPlace": "STANDARD POPULATED PLACE",
            "zipCode": "STANDARD ZIP CODE",
            "district": "STANDARD DISTRICT",
            "residentialArea": "STANDARD RESIDENTAL AREA",
            "street": "STANDARD STREET"
        },
        "number": "122",
        "additionalInformation": null,
        "block": null,
        "entrance": null,
        "floor": null,
        "apartment": null,
        "mailbox": null
    },
    "bankingDetails": {
        "directDebit": false,
        "bankId": envVariables.banks,
        "bic": null,
        "iban": null,
        "declaredConsumption": null,
        preferenceIds: [envVariables.preferences],
        creditRatingId: envVariables.credit_rating
    },
    "relatedCustomers": null,
    "communicationData": [
        {
            "status": "ACTIVE",
            "contactTypeName": "AUTOMATION NAME ",
            contactPurposes: [
                {
                    contactPurposeId: envVariables.contact_purpose,
                    status: "ACTIVE"
                },
                {
                    contactPurposeId: envVariables.billing_purpose,
                    status: "ACTIVE"
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
                    "districtId": envVariables.districts,
                    "residentialAreaId": envVariables.residential_areas,
                    "streetId": envVariables.streets,
                    "streetType": "STREET",
                    "residentialAreaType": "QUARTER"
                },
                "number": "122",
                "additionalInformation": null,
                "block": null,
                "entrance": null,
                "floor": null,
                "apartment": null,
                "mailbox": null
            },
            "communicationContacts": [
                {
                    "sendSms": false,
                    "platformId": null,
                    "status": "ACTIVE",
                    "contactType": "MOBILE_NUMBER",
                    "contactValue": "123123123123"
                },
                {
                    "sendSms": false,
                    "platformId": null,
                    "status": "ACTIVE",
                    "contactType": "LANDLINE_PHONE",
                    "contactValue": "12312312"
                },
                {
                    "sendSms": false,
                    "platformId": null,
                    "status": "ACTIVE",
                    "contactType": "CALL_CENTER",
                    "contactValue": "122"
                },
                {
                    "sendSms": false,
                    "platformId": null,
                    "status": "ACTIVE",
                    "contactType": "FAX",
                    "contactValue": "321"
                },
                {
                    "sendSms": false,
                    "platformId": null,
                    "status": "ACTIVE",
                    "contactType": "EMAIL",
                    "contactValue": "AA@AA.COM"
                },
                {
                    "sendSms": false,
                    "platformId": null,
                    "status": "ACTIVE",
                    "contactType": "WEBSITE",
                    "contactValue": "3121"
                },
                {
                    "sendSms": false,
                    "platformId": envVariables.platform,
                    "status": "ACTIVE",
                    "contactType": "OTHER_PLATFORM",
                    "contactValue": "1231"
                }
            ],
            "contactPersons": []
        }
    ],
    "accountManagers": [],
    "customerAdditionalInformation": null,
    "customerEditContractRequests": []
    };
}