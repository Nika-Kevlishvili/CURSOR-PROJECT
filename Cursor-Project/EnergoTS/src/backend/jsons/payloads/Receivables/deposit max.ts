import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

export function deposit_max(){
  return {
    "paymentDeadline":randomGens.generateTodaysDate('dd-mm-yyyy'),
    "initialAmount": 100,
    "currentAmount": null,
    "currencyId": envVariables.currency,
    "numberOfIncomeAccount": "1",
    "costCentre": "1",
    "customerId": null,
    "depositContractRequest": [],
    "templateIds": [
      {
        "templateId": envVariables.deposit_email_template,
        "templateType": "EMAIL",
      },
      {
        "templateId": envVariables.deposit_email_template,
        "templateType": "DOCUMENT"
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