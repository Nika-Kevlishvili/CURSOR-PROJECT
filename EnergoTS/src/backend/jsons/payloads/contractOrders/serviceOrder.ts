import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type BankingDetails = {
  iban: string | null;
  bankId: string | number;
  directDebit: boolean;
};

type BasicParameters = {
  serviceDetailId: string | number | null;
  customerDetailId: string | number | null;
  customerCommunicationIdForBilling: string | number | null;
  bankingDetails: BankingDetails;
  interestRateId: string | number;
  campaignId: string | number | null;
  prepaymentTermInCalendarDays: string | number;
  orderStatus: string;
  statusModifyDate: string;
  proxies: any[];
  assistingEmployees: any[];
  internalIntermediaries: any[];
  externalIntermediaries: any[];
  relatedEntities: any[];
};

type ServiceParameters = {
  invoicePaymentTermId: string | number | null;
  invoicePaymentTermValue: number | null;
  quantity: number | null;
  formulaVariables: any[];
  pods: any[];
  unrecognizedPods: any[];
  linkedContracts: any[];
  contractTermId: string | number | null;
};

type ServiceOrder = {
  basicParameters: BasicParameters;
  serviceParameters: ServiceParameters;
};

export function serviceOrder(): ServiceOrder {
  return {
    "basicParameters": {
      "serviceDetailId": null,
      "customerDetailId": null,
      "customerCommunicationIdForBilling": null,
      "bankingDetails": {
        "iban": null,
        "bankId": envVariables.banks,
        "directDebit": false
      },
      "interestRateId": envVariables.interest_rate,
      "campaignId": null,
      "prepaymentTermInCalendarDays": envVariables.calendar.id,
      "orderStatus": "CONFIRMED",
      "statusModifyDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
      "proxies": [],
      "assistingEmployees": [],
      "internalIntermediaries": [],
      "externalIntermediaries": [],
      "relatedEntities": []
    },
    "serviceParameters": {
      "invoicePaymentTermId": null,
      "invoicePaymentTermValue": null,
      "quantity": randomGens.getRandomEven(),
      "formulaVariables": [],
      "pods": [],
      "unrecognizedPods": [],
      "linkedContracts": [],
      "contractTermId": null,
    }
  };
}