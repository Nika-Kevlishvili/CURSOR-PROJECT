import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

export const rescheduling_create = {
    "numberOfInstallment": "2",
    "amountOfTheInstallment": null,
    "currencyId": envVariables.currency,
    "replaceInterestRateForLiabilitiesId": null,
    "interestRateForInstalmentsId": null,
    "installmentDueDayOfTheMonth": randomGens.generateTodaysDate(),
    "reschedulingInterestType": "INTEREST_WITH_LAST_INSTALLMENT",
    "customerId": null,
    "customerDetailId": null,
    "customerCommunicationDataId": null,
    "customerCommunicationDataIdForContract": null,
    "customerAssessmentId": null,
    "taskIds": [],
    "saveAndExecute": false,
    "liabilityIdsForRescheduling": [
        null
    ],
    "files": [],
    "templateIds": [
        1128,
        1127
    ],
    "templateRequests": [
    {
      "templateId": 1128,
      "signings": [
        "SIGNING_WITH_SYSTEM_CERTIFICATE",
        "SIGNING_WITH_QUALIFIED_SIGNATURE",
        "SIGNING_WITH_TABLET"
      ],
      "outputFileFormat": [
        "PDF",
        "DOCX"
      ]
    },
    {
      "templateId": 1127,
      "signings": [
        "SIGNING_WITH_SYSTEM_CERTIFICATE",
        "SIGNING_WITH_QUALIFIED_SIGNATURE",
        "SIGNING_WITH_TABLET"
      ],
      "outputFileFormat": [
        "PDF",
        "DOCX"
      ]
    }
  ],
    "reschedulingLpfs": [
        {
            "id": null,
            "interest_default_currency": envVariables.currency,
        }
    ],
    "installments": [
        {
            "fee": 0,
            "due_date": "{{due_date}}",
            "inst_num": 1,
            "inst_name": "inst-1",
            "inst_amount": null,
            "interest_amount": null,
            "principal_amount": null
        },
        {
            "fee": 0,
            "due_date": "{{due_date2}}",
            "inst_num": 2,
            "inst_name": "inst-2",
            "inst_amount": null,
            "interest_amount": null,
            "principal_amount": null
        }
    ],
    "reschedulingStatus": "EXECUTED"
}