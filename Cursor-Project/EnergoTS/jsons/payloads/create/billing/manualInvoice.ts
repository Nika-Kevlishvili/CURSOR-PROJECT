import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

// Types for manual invoice payload
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

type ManualInvoiceSummaryRow = {
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
};

type ManualInvoiceParameters = {
    manualInvoiceBasicDataParameters: {
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
        customerDetailId: string | number | null;
        contractOrderType: string | null;
        contractOrderId: string | number | null;
        billingGroupId: string | number | null;
        invoiceCommunicationDataId: string | number | null;
    };
    manualInvoiceSummaryDataParameters: {
        manualInvoiceType: string;
        summaryDataRowList: ManualInvoiceSummaryRow[];
    };
    manualInvoiceDetailedDataParameters: any | null;
};

type ManualInvoice = {
    commonParameters: CommonParameters;
    billingType: string;
    manualInvoiceParameters: ManualInvoiceParameters;
};

export function manualInvoice(): ManualInvoice {
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
        "invoiceDueDate": "ACCORDING_TO_THE_CONTRACT",
        "dueDate": null,
        "sendingAnInvoice": "ACCORDING_TO_THE_CONTRACT",
        "executionType": "MANUAL",
        "executionDateAndTime": null,
        "accountingPeriodId": envVariables.accounting_period,
        "billingNotifications": []
        },
        "billingType": "MANUAL_INVOICE",
        "manualInvoiceParameters": {
            "manualInvoiceBasicDataParameters": {
                "prefixType": "PRODUCT",
                "basisForIssuing": "MANUAL INVOICE",
                "numberOfIncomeAccount": "22",
                "numberOfIncomeAccountManual": true,
                "costCenterControllingOrder": "2",
                "costCenterControllingOrderManual": true,
                "applicableInterestRateId": envVariables.interest_rate, // from interest rate
                "applicableInterestRateManual": true,
                "vatRateId": null,
                "globalVatRate": true,
                "vatRateManual": true,
                "directDebit": false,
                "directDebitManual": true,
                "bankId": null,
                "iban": null,
                "customerDetailId": null, // from customer
                "contractOrderType": null,
                "contractOrderId": null,
                "billingGroupId": null,
                "invoiceCommunicationDataId": null //from customer
            },
            "manualInvoiceSummaryDataParameters": {
                "manualInvoiceType": "STANDARD_INVOICE",
                "summaryDataRowList": [
                    {
                        "priceComponentOrPriceComponentGroupOrItem": "2",
                        "totalVolumes": "2",
                        "unitOfMeasuresForTotalVolumes": "2",
                        "unitPrice": "2",
                        "unitOfMeasureForUnitPrice": "2",
                        "value": 10,
                        "incomeAccount": "2",
                        "costCenter": "2",
                        "vatRateId": null,
                        "globalVatRate": true,
                        "valueCurrencyId": envVariables.currency
                    }
                ]
            },
            "manualInvoiceDetailedDataParameters": null
            }
    }
}