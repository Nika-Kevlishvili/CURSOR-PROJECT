import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

export function periodicBilling() {
    return {
     "basicParameters": {
        "applicationModelType": [
            "FOR_VOLUMES",
            "OVER_TIME_PERIODICAL",
            "OVER_TIME_ONE_TIME",
            "PER_PIECE",
            "INTERIM_AND_ADVANCE_PAYMENT",
            "WITH_ELECTRICITY_INVOICE"
        ],
        "periodicMaxEndDate": "CURRENT_MONTH",
        "periodicMaxEndDateValue": "1",
        "maxEndDate": null,
        "billingCriteria": "LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS",
        "billingApplicationLevel": "CONTRACT",
        "listOfCustomersContractsOrPOD": null, // from contract
        "customersContractOrPODConditions": null
    },
    "commonParameters": {
        "processPeriodicityIds": [
            null // from periodicity
        ],
        "templateId": null,
        "emailTemplateId": null,
        "periodicity": "PERIODIC",
        "runStages": [],
        "additionalInformation": null,
        "taxEventDate": null,
        "accountingPeriodId": null,
        "invoiceDate": null,
        "invoiceDueDate": "ACCORDING_TO_THE_CONTRACT",
        "dueDate": null,
        "sendingAnInvoice": "ACCORDING_TO_THE_CONTRACT",
        "executionType": null,
        "executionDateAndTime": null,
        "billingNotifications": []
    },
    "interimAndAdvancePaymentParameters": null,
    "billingType": "STANDARD_BILLING",
    "taskId": null
    };
}