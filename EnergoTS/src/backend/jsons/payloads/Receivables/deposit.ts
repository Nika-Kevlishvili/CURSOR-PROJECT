import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type DepositTemplateLink = { templateId: number; templateType: string; }
type PaymentDeadlineAfterWithdrawal = {
  calendarType: string;
  value: number;
  calendarId: number;
  excludeWeekends: boolean;
  excludeHolidays: boolean;
  dueDateChange: unknown;
  name: string;
}

type DepositPayload = {
  paymentDeadline: string;
  initialAmount: number;
  currentAmount: number | null;
  currencyId: number;
  numberOfIncomeAccount: string;
  costCentre: string | null;
  customerId: number | any
  depositContractRequest: unknown[];
  templateIds: DepositTemplateLink[];
  paymentDeadlineAfterWithdrawalRequest: PaymentDeadlineAfterWithdrawal;
  refundDate: string | null;
}

export function deposit(): DepositPayload {
  return {
    "paymentDeadline":randomGens.generateTodaysDate('dd-mm-yyyy'),
    "initialAmount": 100,
    "currentAmount": null,
    "currencyId": envVariables.currency,
    "numberOfIncomeAccount": "1",
    "costCentre": null,
    "customerId": null,
    "depositContractRequest": [],
    "templateIds": [
      {
        "templateId": envVariables.deposit_email_template,
        "templateType": "EMAIL"
      }
    ],
    "paymentDeadlineAfterWithdrawalRequest": {
      "calendarType": "WORKING_DAYS",
      "value": 0,
      "calendarId": envVariables.calendar.id,
      "excludeWeekends": false,
      "excludeHolidays": false,
      "dueDateChange": null,
      "name": "0 Working Days ( )"
    },
    "refundDate": null
  }
}