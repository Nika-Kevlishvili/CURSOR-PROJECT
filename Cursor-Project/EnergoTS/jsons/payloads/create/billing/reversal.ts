import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

// Types for Invoice Reversal payload
type InvoiceReversalParameters = {
    fileId: string | number | null;
    listOfInvoices: string;
};

type CommonParameters = {
    periodicity: string;
    templateId: string | number | null;
    emailTemplateId: string | number | null;
    runStages: Array<string | number>;
    status: string | null;
    sendingAnInvoice: string;
    accountingPeriodId: string | number | null;
    invoiceDate: string;
    executionType: string;
    executionDateAndTime: string | null;
    executionDate: string | null;
    executionTime: string | null;
    additionalInformation: string | null;
    billingNotifications: Array<any>;
    filePrintName: string;
};

type Reversal = {
    invoiceReversalParameters: InvoiceReversalParameters;
    billingType: string;
    commonParameters: CommonParameters;
};

export function reversal(): Reversal {
    return {
        "invoiceReversalParameters": {
            "fileId": null,
            "listOfInvoices": ""
        },
        "billingType": "INVOICE_REVERSAL",
        "commonParameters": {
            "periodicity": "STANDARD",
            "templateId": null,
            "emailTemplateId": null,
            "runStages": [],
            "status": null,
            "sendingAnInvoice": "ACCORDING_TO_THE_CONTRACT",
            "accountingPeriodId": envVariables.accounting_period,
            "invoiceDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
            "executionType": "IMMEDIATELY",
            "executionDateAndTime": null,
            "executionDate": null,
            "executionTime": null,
            "additionalInformation": null,
            "billingNotifications": [],
            "filePrintName": "playwright"
        }
    }
}