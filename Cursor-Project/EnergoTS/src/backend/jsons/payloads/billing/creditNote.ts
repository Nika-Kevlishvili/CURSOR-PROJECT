import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

// Types for manual credit/debit note payload
type CommonParameters = {
    processPeriodicityIds: Array<string | number>;
    periodicity: string;
    runStages: Array<string | number>;
    templateId: string | number | null;
    emailTemplateId: string | number | null;
    additionalInformation: string | null;
    taxEventDate: string;
    invoiceDate: string;
    invoiceDueDate: string;
    dueDate: string | null;
    sendingAnInvoice: string;
    executionType: string;
    executionDateAndTime: string | null;
    accountingPeriodId: string | number | null;
    billingNotifications: Array<any>;
};

type BillingRunInvoiceInformation = {
    invoiceId: string | number | null;
    invoiceNumber: string | number | null;
};

type ManualCreditOrDebitNoteParameters = {
    manualCreditOrDebitNoteBasicDataParameters: {
        prefixType: string | null;
        basisForIssuing: string | null;
        numberOfIncomeAccount: string | null;
        numberOfIncomeAccountManual: boolean;
        costCenterControllingOrder: string | null;
        costCenterControllingOrderManual: boolean;
        applicableInterestRateId: string | number | null;
        applicableInterestRateManual: boolean;
        vatRateId: string | number | null;
        globalVatRate: boolean;
        vatRateManual: boolean;
        directDebit: boolean | null;
        directDebitManual: boolean;
        bankId: string | number | null;
        iban: string | null;
        documentType: string;
        billingRunInvoiceInformationList: BillingRunInvoiceInformation[];
    };
    manualCreditOrDebitNoteSummaryDataParameters: {
        manualInvoiceType: string;
        summaryDataRowList: Array<{
            priceComponentOrPriceComponentGroupOrItem: string | number;
            totalVolumes: string | number;
            unitOfMeasuresForTotalVolumes: string | null;
            unitPrice: string | number;
            unitOfMeasureForUnitPrice: string | null;
            value: number | string;
            incomeAccount: string | null;
            costCenter: string | null;
            vatRateId: string | number | null;
            globalVatRate: boolean;
            valueCurrencyId: string | number | null;
        }>;
    };
    manualCreditOrDebitNoteDetailedDataParameters: any | null;
};

type CreditNote = {
    commonParameters: CommonParameters;
    billingType: string;
    manualCreditOrDebitNoteParameters: ManualCreditOrDebitNoteParameters;
};

export function creditNote(): CreditNote {
    return {
        "commonParameters": {
            "processPeriodicityIds": [],
            "periodicity": "STANDARD",
            "runStages": [],
            "templateId": envVariables.invoice_document_template,
            "emailTemplateId": envVariables.invoice_email_template,
            "additionalInformation": null,
            "taxEventDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
            "invoiceDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
            "invoiceDueDate": "DATE",
            "dueDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
            "sendingAnInvoice": "ACCORDING_TO_THE_CONTRACT",
            "executionType": "MANUAL",
            "executionDateAndTime": null,
            "accountingPeriodId": envVariables.accounting_period,
            "billingNotifications": []
        },
        "billingType": "MANUAL_CREDIT_OR_DEBIT_NOTE",
        "manualCreditOrDebitNoteParameters": {
            "manualCreditOrDebitNoteBasicDataParameters": {
                "prefixType": null,
                "basisForIssuing": "Manual Debit note Invoice",
                "numberOfIncomeAccount": "2",
                "numberOfIncomeAccountManual": true,
                "costCenterControllingOrder": "2",
                "costCenterControllingOrderManual": true,
                "applicableInterestRateId": null, // from interest rate
                "applicableInterestRateManual": true,
                "vatRateId": null,
                "globalVatRate": true,
                "vatRateManual": true,
                "directDebit": false,
                "directDebitManual": true,
                "bankId": null,
                "iban": null,
                "documentType": "DEBIT_NOTE",
                "billingRunInvoiceInformationList": [
                    {
                        "invoiceId": null, //from invoice
                        "invoiceNumber": null // from invoice
                    }
                ]
            },
            "manualCreditOrDebitNoteSummaryDataParameters": {
                "manualInvoiceType": "STANDARD_INVOICE",
                "summaryDataRowList": [
                    {
                        "priceComponentOrPriceComponentGroupOrItem": "2",
                        "totalVolumes": "2",
                        "unitOfMeasuresForTotalVolumes": "2",
                        "unitPrice": "2",
                        "unitOfMeasureForUnitPrice": "2",
                        "value": "{{InvoiceValue}}",
                        "incomeAccount": "2",
                        "costCenter": "2",
                        "vatRateId": null,
                        "globalVatRate": true,
                        "valueCurrencyId": envVariables.currency
                    }
                ]
            },
            "manualCreditOrDebitNoteDetailedDataParameters": null
        }
    };
}