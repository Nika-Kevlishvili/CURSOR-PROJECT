import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

// Types for Standard Billing - For volumes payload
type BasicParameters = {
    applicationModelType: ('FOR_VOLUMES' | 'OVER_TIME_PERIODICAL' | 'OVER_TIME_ONE_TIME' | 'PER_PIECE' | 'INTERIM_AND_ADVANCE_PAYMENT' | 'WITH_ELECTRICITY_INVOICE')[];
    periodicMaxEndDate: string | null;
    periodicMaxEndDateValue: string | number | null;
    maxEndDate?: string | null;
    billingCriteria: string;
    billingApplicationLevel: 'CUSTOMER' | 'CONTRACT' | 'POD';
    listOfCustomersContractsOrPOD: any;
    customersContractOrPODConditions: any;
};

type CommonParameters = {
    processPeriodicityIds: Array<string | number>;
    templateId: string | number | null;
    emailTemplateId: string | number | null;
    periodicity: string;
    runStages: Array<string | number>;
    additionalInformation: string | null;
    taxEventDate: string;
    accountingPeriodId: string | number | null;
    invoiceDate: string;
    invoiceDueDate: string;
    dueDate: string | null;
    sendingAnInvoice: string;
    executionType: string;
    executionDateAndTime: string | null;
    billingNotifications: Array<any>;
    filePrintName: string;
};

type ForVolumes = {
    basicParameters: BasicParameters;
    commonParameters: CommonParameters;
    interimAndAdvancePaymentParameters: any | null;
    billingType: string;
    taskId: string | number | null;
};

export function forVolumes(): ForVolumes {
    return  {
     "basicParameters": {
        "applicationModelType": [
            "FOR_VOLUMES"
        ],
        "periodicMaxEndDate": null,
        "periodicMaxEndDateValue": null,
        "maxEndDate": "",
        "billingCriteria": "LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS",
        "billingApplicationLevel": "CONTRACT",
        "listOfCustomersContractsOrPOD": null, // from contract
        "customersContractOrPODConditions": null
    },
    "commonParameters": {
        "processPeriodicityIds": [],
        "templateId": null,
        "emailTemplateId": null,
        "periodicity": "STANDARD",
        "runStages": [],
        "additionalInformation": null,
        "taxEventDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
        "accountingPeriodId": envVariables.accounting_period,
        "invoiceDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
        "invoiceDueDate": "ACCORDING_TO_THE_CONTRACT",
        "dueDate": null,
        "sendingAnInvoice": "ACCORDING_TO_THE_CONTRACT",
        "executionType": "MANUAL",
        "executionDateAndTime": null,
        "billingNotifications": [],
        "filePrintName": 'playwright'
    },
    "interimAndAdvancePaymentParameters": null,
    "billingType": "STANDARD_BILLING",
    "taskId": null,
    }
}
