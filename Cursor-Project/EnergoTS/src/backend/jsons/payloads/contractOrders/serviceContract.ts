import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type BasicParameters = {
  contractStatus: string;
  detailsSubStatus: string;
  contractVersionStatus: string;
  contractVersionTypes: (string | number)[];
  contractTermUntilAmountIsReached: number | null;
  currencyId: string | number | null;
  contractStatusModifyDate: string;
  serviceId: string | number | null;
  serviceVersionId: number;
  contractType: string;
  contractTermUntilAmountIsReachedCheckbox: boolean;
  signInDate: string;
  entryIntoForceDate: string | null;
  startOfTheInitialTermOfTheContract: string | null;
  terminationDate: string | null;
  contractTermEndDate: string | null;
  perpetuityDate: string | null;
  customerId: string | number | null;
  customerVersionId: number;
  communicationDataForBilling: string | number | null;
  communicationDataForContract: string | number | null;
  relatedEntities: any[];
  files: any[];
  documents: any[];
  proxy: any | null;
};

type BankingDetails = {
  directDebit: boolean;
  bankId: string | number | null;
  iban: string | null;
};

type AdditionalParameters = {
  bankingDetails: BankingDetails;
  interestRateId: string | number;
  campaignId: string | number;
  internalIntermediaries: any[];
  externalIntermediaries: any[];
  assistingEmployees: any[];
};

type ServiceParameters = {
  quantity: number | null;
  contractServiceAdditionalParamsRequests: any[];
  contractType: string | null;
  contractTermId: string | number | null;
  contractTermEndDate: string | null;
  paymentGuarantee: string;
  cashDepositAmount: number | null;
  bankGuaranteeAmount: number | null;
  guaranteeContractInfo: string | null;
  guaranteeContract: boolean;
  contractFormulas: any[];
  invoicePaymentTermId: string | number | null;
  invoicePaymentTerm: number;
  entryIntoForce: string;
  entryIntoForceDate: string;
  startOfContractInitialTerm: string;
  startOfContractInitialTermDate: string | null;
  interimAdvancePaymentsRequests: any[];
  monthlyInstallmentNumber: number | null;
  monthlyInstallmentAmount: number | null;
  podIds: (string | number | null)[];
  unrecognizedPods: any[];
  unrecognizedPodsEditList: any[];
  podsEditList: any[];
  contractNumbers: any[];
  contractNumbersEditList: any[];
};

type ServiceContract = {
  basicParameters: BasicParameters;
  additionalParameters: AdditionalParameters;
  serviceParameters: ServiceParameters;
};

export function Service_contract(): ServiceContract {
    return {
      "basicParameters": {
      "contractStatus": "ENTERED_INTO_FORCE",
      "detailsSubStatus": "AWAITING_ACTIVATION",
      "contractVersionStatus": "SIGNED",
      "contractVersionTypes": [
          envVariables.contract_version_types
      ],
      "contractTermUntilAmountIsReached": null,
      "currencyId": null,
      "contractStatusModifyDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
      "serviceId": null, // from service
      "serviceVersionId": 1,
      "contractType": "CONTRACT",
      "contractTermUntilAmountIsReachedCheckbox": false,
      "signInDate": '2025-01-01',
      "entryIntoForceDate": '2025-01-01',
      "startOfTheInitialTermOfTheContract": null,
      "terminationDate": null,
      "contractTermEndDate": null,
      "perpetuityDate": null,
      "customerId": null, // from customer
      "customerVersionId": 1,
      "communicationDataForBilling": null, // from customer
      "communicationDataForContract": null, // from customer
      "relatedEntities": [],
      "files": [],
      "documents": [],
      "proxy": null
    },
    "additionalParameters": {
      "bankingDetails": {
        "directDebit": false,
        "bankId": null,
        "iban": null
      },
      "interestRateId": envVariables.interest_rate,
      "campaignId": envVariables.campaign,
      "internalIntermediaries": [],
      "externalIntermediaries": [],
      "assistingEmployees": []
    },
    "serviceParameters": {
      "quantity": null,
      "contractServiceAdditionalParamsRequests": [],
      "contractType": null,
      "contractTermId": null,
      "contractTermEndDate": null,
      "paymentGuarantee": "NO",
      "cashDepositAmount": null,
      "bankGuaranteeAmount": null,
      "guaranteeContractInfo": null,
      "guaranteeContract": false,
      "contractFormulas": [],
      "invoicePaymentTermId": null, // from terms
      "invoicePaymentTerm": 10,
      "entryIntoForce": "MANUAL",
      "entryIntoForceDate":  randomGens.generateTodaysDate('yyyy-mm-dd'),
      "startOfContractInitialTerm": "MANUAL",
      "startOfContractInitialTermDate": null,
      "interimAdvancePaymentsRequests": [],
      "monthlyInstallmentNumber": null,
      "monthlyInstallmentAmount": null,
      "podIds": [], // from pod
      "unrecognizedPods": [],
      "unrecognizedPodsEditList": [],
      "podsEditList": [],
      "contractNumbers": [],
      "contractNumbersEditList": []
    }
  };
}