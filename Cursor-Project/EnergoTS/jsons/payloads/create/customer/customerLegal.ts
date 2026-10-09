import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type Status = 'ACTIVE' | 'INACTIVE' | 'NEW';

type BusinessCustomerDetails = {
    procurementLaw: boolean;
    name: string;
    nameTranslated: string;
    legalFormId: number;
    legalFormTransId: number;
}

type LocalAddressDataFull = {
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

type AddressFull = {
    foreign: boolean;
    localAddressData: LocalAddressDataFull;
    number: string | null;
    additionalInformation: string | null;
    block: number | null;
    entrance: string | null;
    floor: string | null;
    apartment: string | null;
    mailbox: string | null;
    longitude?: number;
    latitude?: number;
}

type BankingDetails = {
    directDebit: boolean;
    bankId: number;
    bic: string | null;
    iban: string | null;
    declaredConsumption: number | null;
    preferenceIds: number[];
    creditRatingId: number;
}

type Manager = {
    id: number | null;
    titleId: number;
    name: string;
    middleName: string | null;
    surname: string;
    personalNumber: string | null;
    unrecognizedIdentifier: boolean;
    jobPosition: string;
    positionHeldFrom: string | null;
    positionHeldTo: string | null;
    birthDate: string | null;
    representationMethodId: number;
    additionalInformation: string | null;
    status: Status;
}

type CommunicationPurpose = {
    contactPurposeId: number;
    status: Status;
}

type LocalAddressDataShort = {
    countryId: number;
    regionId: number;
    municipalityId: number;
    populatedPlaceId: number;
    zipCodeId: number;
    districtId?: number;
    residentialAreaId?: number;
    streetId?: number;
    streetType?: string;
    residentialAreaType?: string;
}

type AddressShort = {
    foreign: boolean;
    localAddressData: LocalAddressDataShort;
    number: string | null;
    additionalInformation: string | null;
    block: number | null;
    entrance: string | null;
    floor: string | null;
    apartment: string | null;
    mailbox: string | null;
    longitude?: number;
    latitude?: number;
}

type CommunicationContact = {
    sendSms: boolean;
    platformId: number | null;
    status: Status;
    contactType: 'MOBILE_NUMBER' | 'EMAIL' | string;
    contactValue: string;
}

type CommunicationDataEntry = {
    status: Status;
    contactTypeName: string;
    contactPurposes: CommunicationPurpose[];
    address: AddressShort;
    communicationContacts: CommunicationContact[];
    contactPersons: unknown[];
}

type CustomerLegalPayload = {
    customerType: 'LEGAL_ENTITY';
    customerIdentifier: string;
    foreign: boolean;
    marketingConsent: boolean;
    preferCommunicationInEnglish: boolean;
    oldCustomerNumber: string | null;
    vatNumber: string | null;
    customerDetailStatus: Status;
    businessCustomerDetails: BusinessCustomerDetails;
    ownershipFormId: number;
    economicBranchId: number;
    economicBranchNCEAId: number | null;
    mainSubjectOfActivity: string;
    segmentIds: number[];
    address: AddressFull;
    bankingDetails: BankingDetails;
    managers: Manager[];
    relatedCustomers: unknown;
    owner: unknown;
    communicationData: CommunicationDataEntry[];
    accountManagers: unknown;
    customerEditContractRequests: unknown[];
}

export function customerLegal(): CustomerLegalPayload {
    return {
        customerType: "LEGAL_ENTITY",
        customerIdentifier: randomGens.generateUniqueIdentifier(),
        foreign: true,
        marketingConsent: false,
        preferCommunicationInEnglish: false,
        oldCustomerNumber: null,
        vatNumber: null,
        customerDetailStatus: "NEW",
        businessCustomerDetails: {
            procurementLaw: false,
            name: "AUTOMATION",
            nameTranslated: "AUTOMATION",
            legalFormId: envVariables.legal_forms,
            legalFormTransId: envVariables.legal_forms
        },
        ownershipFormId: envVariables.form_of_ownership,
        economicBranchId: envVariables.economic_branch_based_on_commercial_information,
        economicBranchNCEAId: null,
        mainSubjectOfActivity: "MainSubject",
        segmentIds: [envVariables.segment],
        address: {
            foreign: false,
            localAddressData: {
                countryId: envVariables.countries,
                regionId: envVariables.regions,
                municipalityId: envVariables.municipalities,
                populatedPlaceId: envVariables.population_places,
                zipCodeId: envVariables.zip_codes,
                districtId: envVariables.districts,
                residentialAreaId: envVariables.residential_areas,
                streetId: envVariables.streets,
                streetType: "STREET",
                residentialAreaType: "QUARTER"
            },
            number: "122",
            additionalInformation: null,
            block: null,
            entrance: null,
            floor: null,
            apartment: null,
            mailbox: null
        },
        bankingDetails: {
            directDebit: false,
            bankId: envVariables.banks,
            bic: null,
            iban: null,
            declaredConsumption: null,
            preferenceIds: [envVariables.preferences],
            creditRatingId: envVariables.credit_rating
        },
        managers: [
            {
                id: null,
                titleId: envVariables.title,
                name: "NAME",
                middleName: null,
                surname: "SURNAME",
                personalNumber: null,
                unrecognizedIdentifier: false,
                jobPosition: "QA",
                positionHeldFrom: null,
                positionHeldTo: null,
                birthDate: null,
                representationMethodId: envVariables.method_of_representation,
                additionalInformation: null,
                status: "ACTIVE"
            }
        ],
        relatedCustomers: null,
        owner: null,
        communicationData: [
            {
                status: "ACTIVE",
                contactTypeName: "MAIL",
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
                address: {
                    foreign: false,
                    localAddressData: {
                        countryId: envVariables.countries,
                        regionId: envVariables.regions,
                        municipalityId: envVariables.municipalities,
                        populatedPlaceId: envVariables.population_places,
                        zipCodeId: envVariables.zip_codes,
                        streetId: envVariables.streets,
                        streetType: "STREET",

                    },
                    number: "122",
                    additionalInformation: null,
                    block: null,
                    entrance: null,
                    floor: null,
                    apartment: null,
                    mailbox: null,
                    latitude: 90,
                    longitude: 180
                },
                communicationContacts: [
                    {
                        sendSms: true,
                        platformId: envVariables.platform,
                        status: "ACTIVE",
                        contactType: "MOBILE_NUMBER",
                        contactValue: "555125531"
                    },
                    {
                        sendSms: true,
                        platformId: envVariables.platform,
                        status: "ACTIVE",
                        contactType: "EMAIL",
                        contactValue: "test@test.com"
                    }
                ],
                contactPersons: []
            }
        ],
        accountManagers: null,
        customerEditContractRequests: []
    };
}