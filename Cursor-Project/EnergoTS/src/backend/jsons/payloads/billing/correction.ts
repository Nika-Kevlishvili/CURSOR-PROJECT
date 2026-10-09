import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

// Types for Standard Billing - Invoice correction payload
type CommonParameters = {
  templateId: string | number | null;
  emailTemplateId: string | number | null;
  runStages: Array<string | number>;
  additionalInformation: string | null;
  taxEventDate: string;
  accountingPeriodId: string | number | null;
  invoiceDate: string;
  invoiceDueDate: string;
  sendingAnInvoice: string;
  executionType: string;
  executionDateAndTime: string | null;
  billingNotifications: Array<any>;
  filePrintName: string;
};

type InvoiceCorrectionParameters = {
  priceChange: boolean;
  volumeChange: boolean;
  listOfInvoices: any; // invoice number(s)
  fileId: string | number | null;
};

type InvoiceCorrection = {
  commonParameters: CommonParameters;
  invoiceCorrectionParameters: InvoiceCorrectionParameters;
  billingType: string;
};

export function invoiceCorrection(): InvoiceCorrection {
    return {
      "commonParameters": {
        "templateId": null,
        "emailTemplateId": null,
        "runStages": [],
        "additionalInformation": null,
        "taxEventDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
        "accountingPeriodId": envVariables.accounting_period,
        "invoiceDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
        "invoiceDueDate": "ACCORDING_TO_THE_CONTRACT",
        "sendingAnInvoice": "ACCORDING_TO_THE_CONTRACT",
        "executionType": "MANUAL",
        "executionDateAndTime": null,
        "billingNotifications": [],
        "filePrintName": "playwright"
    },
    "invoiceCorrectionParameters": {
      "priceChange": true,
      "volumeChange": false,
      "listOfInvoices": null, // invoice number
      "fileId": null
    },
    "billingType": "INVOICE_CORRECTION"
  };
}