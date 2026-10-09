import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type BasicSettings = {
  name: string;
  nameTransliterated: string;
  printingName: string;
  printingNameTransliterated: string;
  serviceGroupId: string | number;
  otherSystemConnectionCode: string | null;
  serviceTypeId: string | number;
  saleMethods: string[];
  paymentGuarantees: string[];
  cashDepositAmount: number | null;
  bankGuaranteeAmount: number | null;
  typePointsOfDelivery: any[];
  consumptionPurposes: any[];
  podMeteringTypes: any[];
  voltageLevels: any[];
  serviceDetailStatus: string;
  availableForSale: boolean;
  availableFrom: string | null;
  availableTo: string | null;
  shortDescription: string;
  fullDescription: string | null;
  invoiceAndTemplatesText: string | null;
  invoiceAndTemplatesTextTransliterated: string | null;
  incomeAccountNumber: string;
  costCenterControllingOrder: string;
  vatRateId: string | number | null;
  serviceUnitId: string | number;
  salesChannels: any[] | null;
  salesAreas: any[] | null;
  segments: any[] | null;
  gridOperators: any[] | null;
  isIndividual: boolean;
  customerIdentifier: string | null;
  capacityLimitType: string | null;
  globalVatRate: boolean;
  globalGridOperator: boolean;
  globalSalesChannel: boolean;
  globalSalesAreas: boolean;
  globalSegment: boolean;
  preferenceIds: any[];
};

type PriceSettings = {
  equalMonthlyInstallmentsActivation: boolean;
  installmentNumber: number | null;
  installmentNumberFrom: number | null;
  installmentNumberTo: number | null;
  amount: number | null;
  amountFrom: number | null;
  amountTo: number | null;
};

type AdditionalSettings = {
  periodicity: string;
  paymentMethod: string;
  paymentBeforeExecution: boolean;
  executionLevel: string;
  ineligiblePaymentChannels: any[] | null;
  additionalField1: string | null;
  additionalField2: string | null;
  additionalField3: string | null;
  additionalField4: string | null;
  additionalField5: string | null;
  additionalField6: string | null;
  additionalField7: string | null;
  additionalField8: string | null;
  additionalField9: string | null;
  additionalField10: string | null;
  serviceAdditionalParams: any[];
  autoSubscriptionRenewal: boolean;
};

type ContractTerm = {
  id: string | number | null;
  name: string;
  periodType: string;
  termType: string | null;
  value: number | null;
  perpetuityCause: boolean;
  automaticRenewal: boolean | null;
  numberOfRenewals: number | null;
  termFromGroup: boolean;
  renewalPeriodValue: number | null;
  renewalPeriodType: string | null;
};

type TemplateId = {
  templateId: string | number;
  templateType: string;
};

type Service = {
  basicSettings: BasicSettings;
  priceSettings: PriceSettings;
  additionalSettings: AdditionalSettings;
  term: any | null;
  interimAdvancePayments: any[];
  interimAdvancePaymentGroups: any[];
  priceComponents: any[];
  priceComponentGroups: any[];
  termGroup: any | null;
  terminations: any[];
  terminationGroups: any[];
  penalties: any[];
  penaltyGroups: any[];
  relatedEntities: any[];
  contractTerms: ContractTerm[];
  serviceFiles: any[];
  templateIds: TemplateId[];
};

export function service(): Service {
  return {
    "basicSettings": {
      "name": `Service${randomGens.generateCurrentTimeStamp()}`,
      "nameTransliterated": randomGens.generateRandomString(true, false, 10),
      "printingName": randomGens.generateRandomString(true, false, 10),
      "printingNameTransliterated": randomGens.generateRandomString(true, false, 10),
      "serviceGroupId": envVariables.service_group,
      "otherSystemConnectionCode": null,
      "serviceTypeId": envVariables.service_type,
      "saleMethods": [
        "CONTRACT",
        "ORDER"
      ],
      "paymentGuarantees": [
        "NO"
      ],
      "cashDepositAmount": null,
      "bankGuaranteeAmount": null,
      "typePointsOfDelivery": [],
      "consumptionPurposes": [],
      "podMeteringTypes": [],
      "voltageLevels": [],
      "serviceDetailStatus": "ACTIVE",
      "availableForSale": true,
      "availableFrom": null,
      "availableTo": null,
      "shortDescription": "Description for automation service",
      "fullDescription": null,
      "invoiceAndTemplatesText": null,
      "invoiceAndTemplatesTextTransliterated": null,
      "incomeAccountNumber": "2",
      "costCenterControllingOrder": "2",
      "vatRateId": null,
      "serviceUnitId": envVariables.service_unit,
      "salesChannels": null,
      "salesAreas": null,
      "segments": null,
      "gridOperators": null,
      "isIndividual": false,
      "customerIdentifier": null,
      "capacityLimitType": null,
      "globalVatRate": true,
      "globalGridOperator": true,
      "globalSalesChannel": true,
      "globalSalesAreas": true,
      "globalSegment": true,
      "preferenceIds": [envVariables.preferences]
    },
    "priceSettings": {
      "equalMonthlyInstallmentsActivation": false,
      "installmentNumber": null,
      "installmentNumberFrom": null,
      "installmentNumberTo": null,
      "amount": null,
      "amountFrom": null,
      "amountTo": null
    },
    "additionalSettings": {
      "periodicity": "ONE_TIME",
      "paymentMethod": "ONE_TIME",
      "paymentBeforeExecution": false,
      "executionLevel": "POINT_OF_DELIVERY",
      "ineligiblePaymentChannels": null,
      "additionalField1": null,
      "additionalField2": null,
      "additionalField3": null,
      "additionalField4": null,
      "additionalField5": null,
      "additionalField6": null,
      "additionalField7": null,
      "additionalField8": null,
      "additionalField9": null,
      "additionalField10": null,
      "serviceAdditionalParams": [],
      "autoSubscriptionRenewal": false
    },
    "term": null,
    "interimAdvancePayments": [],
    "interimAdvancePaymentGroups": [],
    "priceComponents": [],
    "priceComponentGroups": [],
    "termGroup": null,
    "terminations": [],
    "terminationGroups": [],
    "penalties": [],
    "penaltyGroups": [],
    "relatedEntities": [],
    "contractTerms": [
      {
        "id": null,
        "name": "Other",
        "periodType": "OTHER",
        "termType": null,
        "value": null,
        "perpetuityCause": true,
        "automaticRenewal": null,
        "numberOfRenewals": null,
        "termFromGroup": false,
        "renewalPeriodValue": null,
        "renewalPeriodType": null
      }
    ],
    "serviceFiles": [],
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
            "templateId": envVariables.service_contract_bulgarian,
            "templateType": "CONTRACT_TEMPLATE"
          },
          {
            "templateId": envVariables.service_contract_bilingual,
            "templateType": "BI_CONTRACT_TEMPLATE"
          }
      ]
  }
}