import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type PaymentTerm = {
  id: string | number | null;
  name: string;
  type: string;
  value: string;
  calendarId: string | number;
  excludes: string[];
  dueDateChange: string;
};

type BasicParameters = {
  directDebit: boolean;
  bankId: string | number;
  iban: string | null;
  interestRateId: string | number;
  paymentTermInCalendarDays: string;
  campaignId: string | number | null;
  customerDetailId: string | number | null;
  customerCommunicationIdForBilling: string | number | null;
  proxy: any | null;
  emailTemplateId: number;
  invoiceTemplateId: number;
  paymentTerm: PaymentTerm;
  internalIntermediaries: any[];
  externalIntermediaries: any[];
  assistingEmployees: any[];
  relatedEntities: any[];
  noInterestInOverdueDebts: boolean;
};

type Good = {
  id: string | number | null;
  goodsDetailId: string | number | null;
  name: string;
  codeForConnectionWithOtherSystem: string | null;
  goodsUnitId: string | number;
  quantity: string;
  price: string;
  currencyId: string | number;
  numberOfIncomingAccount: string;
  costCenterOrControllingOrder: string | null;
  inputDisabled: boolean;
};

type GoodsParameters = {
  numberOfIncomeAccount: string | null;
  costCenterOrControllingOrder: string;
  vatRateId: string | number;
  goods: Good[];
  isGlobalVatRate: boolean;
};

type GoodsOrder = {
  basicParameters: BasicParameters;
  goodsParameters: GoodsParameters;
  orderStatus: string;
  statusModifyDate: string;
};

export function goodsOrder(): GoodsOrder {
    return {
  "basicParameters": {
    "directDebit": false,
    "bankId": envVariables.banks,
    "iban": null,
    "interestRateId": envVariables.interest_rate,
    "paymentTermInCalendarDays": "30",
    "campaignId": null,
    "customerDetailId": null,
    "customerCommunicationIdForBilling": null,
    "proxy": null,
    "emailTemplateId": envVariables.invoice_email_template,
    "invoiceTemplateId": envVariables.invoice_document_template,
    "paymentTerm": {
      "id": null,
      "name": "10 CALENDAR_DAYS (Exclude weekends, Next working day)",
      "type": "CALENDAR_DAYS",
      "value": "10",
      "calendarId": envVariables.calendar.id,
      "excludes": [
        "WEEKENDS"
      ],
      "dueDateChange": "NEXT_WORKING_DAY"
    },
    "internalIntermediaries": [],
    "externalIntermediaries": [],
    "assistingEmployees": [],
    "relatedEntities": [],
    "noInterestInOverdueDebts": false
  },
  "goodsParameters": {
    "numberOfIncomeAccount": null,
    "costCenterOrControllingOrder": "50",
    "vatRateId": envVariables.vat_rate,
    "goods": [
      {
        "id": null,
        "goodsDetailId": null,
        "name": 'GOODS_ORDER',
        "codeForConnectionWithOtherSystem": null,
        "goodsUnitId": envVariables.goods_unit,
        "quantity": randomGens.getRandomEven().toString(),
        "price": randomGens.getRandomEven().toString(),
        "currencyId": envVariables.currency,
        "numberOfIncomingAccount": '1',
        "costCenterOrControllingOrder": '1',
        "inputDisabled": false
      }
    ],
    "isGlobalVatRate": false
  },
  "orderStatus": "CONFIRMED",
  "statusModifyDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
  };
}