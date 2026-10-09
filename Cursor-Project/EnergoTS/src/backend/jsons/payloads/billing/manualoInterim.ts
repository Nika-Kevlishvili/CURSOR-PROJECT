import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

// Types for manual interim & advance payment payload
type CommonParameters = {
    processPeriodicityIds: Array<string | number>;
    periodicity: string;
    runStages: Array<string | number>;
    additionalInformation: string | null;
    taxEventDate: string;
    invoiceDate: string;
    invoiceDueDate: string;
    dueDate: string | null;
    sendingAnInvoice: string;
    executionType: string;
    templateId: string | number | null;
    emailTemplateId: string | number | null;
    executionDateAndTime: string | null;
    accountingPeriodId: string | number | null;
    billingNotifications: Array<any>;
};

type InterimAndAdvancePaymentParameters = {
    amountExcludingVat: string | number;
    currencyId: string | number | null;
    issuingForTheMonthToCurrent: string;
    deductionFrom: string;
    basisForIssuing: string | null;
    prefixType: string | null;
    invoiceDueDateType: string;
    executionDate: string | null;
    executionTime: string | null;
    numberOfIncomeAccount: string | null;
    numberOfIncomeAccountManual: boolean;
    costCenterControllingOrder: string | null;
    costCenterControllingOrderManual: boolean;
    applicableInterestRateId: string | number | null;
    applicableInterestRateManual: boolean;
    directDebit: boolean | null;
    directDebitManual: boolean;
    bankId: string | number | null;
    iban: string | null;
    vatRateManual: boolean;
    vatRateId: string | number | null;
    globalVatRate: boolean;
    issuedSeparateInvoices: string[];
    customerDetailId: string | number | null;
    invoiceCommunicationDataId: string | number | null;
};

type ManualInterim = {
    commonParameters: CommonParameters;
    billingType: string;
    interimAndAdvancePaymentParameters: InterimAndAdvancePaymentParameters;
};

export function Manualinterim(): ManualInterim {
    return {
    "commonParameters": {
        "processPeriodicityIds": [],
        "periodicity": "STANDARD",
        "runStages": [],
        "additionalInformation": null,
        "taxEventDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
        "invoiceDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
        "invoiceDueDate": "DATE",
        "dueDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
        "sendingAnInvoice": "PAPER",
        "executionType": "MANUAL",
        "templateId": envVariables.invoice_document_template,
        "emailTemplateId": envVariables.invoice_email_template,
        "executionDateAndTime": null,
        "accountingPeriodId": envVariables.accounting_period,
        "billingNotifications": []
    },
    "billingType": "MANUAL_INTERIM_AND_ADVANCE_PAYMENT",
    "interimAndAdvancePaymentParameters": {
        "amountExcludingVat": "100",
        "currencyId":envVariables.currency,
        "issuingForTheMonthToCurrent": "ZERO",
        "deductionFrom": "FIRST_INVOICE_FOR_SAME_PERIOD",
        "basisForIssuing": "2",
        "prefixType": "PRODUCT",
        "invoiceDueDateType": "DATE",
        "executionDate": null,
        "executionTime": null,
        "numberOfIncomeAccount": "2",
        "numberOfIncomeAccountManual": true,
        "costCenterControllingOrder": "2",
        "costCenterControllingOrderManual": true,
        "applicableInterestRateId": null, // from interest rate
        "applicableInterestRateManual": true,
        "directDebit": null,
        "directDebitManual": true,
        "bankId": null,
        "iban": null,
        "vatRateManual": true,
        "vatRateId": null,
        "globalVatRate": true,
        "issuedSeparateInvoices": [
            "INVOICE_ONE"
        ],
        "customerDetailId": null, //from customer
        "invoiceCommunicationDataId": null //from customer
        }
    };
}