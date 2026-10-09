import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

let firstNameRand = "NAME-" + randomGens.generateRandomString(true,false,6)
let lastNameRand = "LASTNAME-" + randomGens.generateRandomString(true,false,6)
const businessName = "BUSINESSACT-" + randomGens.generateRandomString(true,false,6)


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
  districtId: number | null;
  residentialAreaId: number | null;
  streetId: number | null;
  residentialAreaType: string | null;
  streetType: string | null;
}

type ForeignAddressData = {
  countryId: number;
  region: string;
  municipality: string;
  populatedPlace: string;
  zipCode: string;
  district: string | null;
  residentialArea: string | null;
  street: string | null;
  residentialAreaType: string | null;
  streetType: string | null;
}

type Address = {
  foreign: boolean;
  localAddressData: LocalAddressData;
  foreignAddressData: ForeignAddressData | null;
  number: string | null;
  additionalInformation: string | null;
  block: string | null;
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
    district: string | null;
    residentialArea: string | null;
    street: string | null;
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
  bankId: number;
  bic: number | string | null;
  iban: string | null;
  declaredConsumption: number | null;
  preferenceIds: number[];
  creditRatingId: number | string;
} | null;

type RelatedCustomer = {
  id: number | null;
  identifier: string;
  relatedCustomerId: number;
  name: string;
  ciConnectionTypeId: number;
  status: Status;
}

type CommunicationPurpose = { contactPurposeId: number; status: Status; }
type CommunicationContact = {
  sendSms: boolean;
  platformId: number | null;
  status: Status;
  contactType: string;
  contactValue: string;
}
type CommunicationAddress = { foreign: boolean; localAddressData: LocalAddressData; number: string | null; additionalInformation: string | null; block: string | null; entrance: string | null; floor: string | null; apartment: string | null; mailbox: string | null; }
type ContactPerson = {
  id: number | null;
  titleId: number;
  name: string;
  middleName: string | null;
  surname: string;
  jobPosition: string | null;
  positionHeldFrom: string | null;
  positionHeldTo: string | null;
  birthDate: string | null;
  additionalInformation: string | null;
  status: Status;
}
type CommunicationDataEntry = {
  status: Status;
  contactTypeName: string;
  contactPurposes: CommunicationPurpose[];
  address: CommunicationAddress;
  communicationContacts: CommunicationContact[];
  contactPersons: ContactPerson[];
}

type AccountManager = { accountManagerId: number; accountManagerTypeId: number; }

type Manager = {
  id: number | null;
  titleId: number;
  name: string;
  middleName: string | null;
  surname: string;
  personalNumber: string | null;
  jobPosition: string;
  positionHeldFrom: string | null;
  positionHeldTo: string | null;
  birthDate: string | null;
  representationMethodId: number;
  additionalInformation: string | null;
  status: Status;
}

type Owner = {
  id: number | null;
  personalNumber: string | null;
  name: string | null;
  additionalInformation: string | null;
  belongingOwnerCapitalId: number;
}

type BusinessCustomerDetails = {
  procurementLaw: boolean;
  name: string;
  nameTranslated: string;
  legalFormId: number;
  legalFormTransId: number;
}

type CustomerPrivateBusinessPayload = {
  customerType: 'PRIVATE_CUSTOMER';
  customerIdentifier: string;
  foreign: boolean;
  marketingConsent: boolean;
  oldCustomerNumber: string | null;
  vatNumber: string | null;
  customerDetailStatus: Status;
  businessActivity: boolean;
  preferCommunicationInEnglish: boolean;
  privateCustomerDetails: PrivateCustomerDetails;
  segmentIds: number[];
  address: Address;
  addressTransl: AddressTransl;
  bankingDetails: BankingDetails;
  relatedCustomers: RelatedCustomer[] | null;
  communicationData: CommunicationDataEntry[] | any;
  accountManagers: AccountManager[] | null;
  customerAdditionalInformation: string | null;
  managers: Manager[] | null;
  owner: Owner[] | null;
  ownershipFormId: number;
  economicBranchId: number;
  economicBranchNCEAId: number | null;
  mainSubjectOfActivity: string;
  businessCustomerDetails: Omit<BusinessCustomerDetails, 'legalFormId' | 'legalFormTransId'> & { legalFormId: number | null; legalFormTransId: number | null };
  customerEditContractRequests: unknown[];
}



export function customerPrivateBusiness(): CustomerPrivateBusinessPayload {
  return {
    "customerType": "PRIVATE_CUSTOMER",
    "customerIdentifier": '',
    "foreign": true,
    "marketingConsent": false,
    "oldCustomerNumber": null,
    "vatNumber": null as string | null,
    "customerDetailStatus": "NEW",
    "businessActivity": true,
    "preferCommunicationInEnglish": false,
    "privateCustomerDetails": {
      "gdprRegulationConsent": false,
      "firstName": firstNameRand,
      "firstNameTranslated": firstNameRand,
      "middleName": null,
      "middleNameTranslated": null,
      "lastName": lastNameRand,
      "lastNameTranslated": lastNameRand
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
      "residentialAreaType": null,
      "streetType": null
    },
      "foreignAddressData": {
    "countryId": envVariables.countries,
          "region": "REGION",
          "municipality": "MUNICIPALITY",
          "populatedPlace": "PPOPULATEDPLACE",
          "zipCode": "ZIPCODE",
      "district": null,
      "residentialArea": null,
      "street": null,
      "residentialAreaType": null,
      "streetType": null
      },
    "number": null,
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
      "number": null,
      "additionalInformation": null,
      "block": null,
      "entrance": null,
      "floor": null,
      "apartment": null,
      "mailbox": null
    },
    "bankingDetails": {
      "directDebit":  true,
      "bankId": envVariables.banks,
      "bic": envVariables.banks,
      "iban": 'BG11AAAA111111AAAAAAAA',
      "declaredConsumption": null,
      "preferenceIds": [
        envVariables.preferences
      ],
      "creditRatingId": envVariables.credit_rating
    },
    "relatedCustomers": [
      {
        "id": null,
        "identifier": "202501019825",
        "relatedCustomerId": 6967383,
        "name": "202501019825 (NAME MIDDLE NAME LAST NAME)",
        "ciConnectionTypeId": envVariables.type_of_connection_for_ci,
        "status": "ACTIVE"
      }
    ],
    "communicationData": [
      {
        "status": "ACTIVE",
        "contactTypeName": "COMMUNICATION" + firstNameRand,
        "contactPurposes": [
          {
            "contactPurposeId": 83,
            "status": "ACTIVE"
          },
          {
            "contactPurposeId": 84,
            "status": "ACTIVE"
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
        "communicationContacts": [ 
          {
            "sendSms": true,
            "platformId": null,
            "status": "ACTIVE",
            "contactType": "MOBILE_NUMBER",
            "contactValue": "55555555555555"
          } ,
          {
            "sendSms": false,
            "platformId": null,
            "status": "ACTIVE",
            "contactType": "LANDLINE_PHONE",
            "contactValue": "5"
          } ,
          {
            "sendSms": false,
            "platformId": null,
            "status": "ACTIVE",
            "contactType": "CALL_CENTER",
            "contactValue": "5626"
          } ,
          {
            "sendSms": false,
            "platformId": null,
            "status": "ACTIVE",
            "contactType": "FAX",
            "contactValue": "5"
          } ,
          {
            "sendSms": false,
            "platformId": null,
            "status": "ACTIVE",
            "contactType": "EMAIL",
            "contactValue": "aaa@asd.co"
          } ,
          {
            "sendSms": false,
            "platformId": null,
            "status": "ACTIVE",
            "contactType": "WEBSITE",
            "contactValue": "asdas"
          } ,
          {
            "sendSms": false,
            "platformId": envVariables.platform,
            "status": "ACTIVE",
            "contactType": "OTHER_PLATFORM",
            "contactValue": "4"
          } 
        ],
        "contactPersons": [
          {
            "id": null,
            "titleId": envVariables.title,
            "name": "NAME",
            "middleName": null,
            "surname": "SURNAME",
            "jobPosition": null,
            "positionHeldFrom": null,
            "positionHeldTo": null,
            "birthDate": null,
            "additionalInformation": null,
            "status": "ACTIVE"
          }
      ]
      }
    ],
    "accountManagers": [
      {
        "accountManagerId": 108,
        "accountManagerTypeId": envVariables.account_manager_types
      } 
    ],
    "customerAdditionalInformation": "ADDINFO",
    "managers": [
      {
        "id": null,
        "titleId": envVariables.title,
        "name": "MANAGER-NAME",
        "middleName": null,
        "surname": "MANAGER-SURNAME",
        "personalNumber": null,
        "jobPosition": "JobPosition",
        "positionHeldFrom": null,
        "positionHeldTo": null,
        "birthDate": null,
        "representationMethodId": envVariables.method_of_representation,
        "additionalInformation": null,
        "status": "ACTIVE"
      }
    ],
    "owner": [
      {
        "id": null,
        "personalNumber": "202501019825",
        "name": "202501019825 (NAME MIDDLE NAME LAST NAME)",
        "additionalInformation": 'AdditionalInformation',
        "belongingOwnerCapitalId": envVariables.belonging_capital_owners
      }
    ],
    "ownershipFormId": envVariables.form_of_ownership,
    "economicBranchId": envVariables.economic_branch_based_on_commercial_information,
    "economicBranchNCEAId": envVariables.economic_branch_according_to_national_classification,
    "mainSubjectOfActivity": "MAINSUBJECT",
    "businessCustomerDetails": {
      "procurementLaw": false,
      "name": businessName,
      "nameTranslated": businessName,
      "legalFormId": envVariables.legal_forms,
      "legalFormTransId": envVariables.legal_forms
    },
    "customerEditContractRequests": []
    };
}


