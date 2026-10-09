import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type PenaltyPaymentTermRequest = {
    calendarType: string;
    name: string;
    value: string | number;
    calendarId: number;
    excludeWeekends: boolean;
    excludeHolidays: boolean;
    dueDateChange: unknown;
}

type PenaltyPayload = {
    name: string;
    contractClauseNumber: string;
    amountFormula: string;
    additionalInformation: string | null;
    penaltyReceivingParties: string[];
    penaltyApplicability: string; // e.g. 'CONTRACT'
    automaticSubmission: boolean;
    variables: unknown;
    minAmount: string;
    maxAmount: string;
    noInterestOnOverdueDebts: boolean;
    actionTypeList: number[];
    currencyId: number;
    penaltyPaymentTermRequest: PenaltyPaymentTermRequest;
    priceComponentTagVariablesInfo: unknown[];
    templateId: number;
    emailTemplateId: number;
}

export function penalty(): PenaltyPayload {
    return {
        "name": `Penalty ${randomGens.generateRandomString(false, true, 10)}`,
        "contractClauseNumber": "Automation",
        "amountFormula": "100",
        "additionalInformation": null,
        "penaltyReceivingParties": [
            "ENERGO_PRO",
            "CUSTOMER"
        ],
        "penaltyApplicability": "CONTRACT",
        "automaticSubmission": true,
        "variables": null,
        "minAmount": "100",
        "maxAmount": "1000",
        "noInterestOnOverdueDebts": false,
        "actionTypeList": [
            4,
            2,
            1,
            3
        ],
        "currencyId": envVariables.currency,
        "penaltyPaymentTermRequest": {
            "calendarType": "WORKING_DAYS",
            "name": "10 WORKING_DAYS ( )",
            "value": "10",
            "calendarId": envVariables.calendar.id,
            "excludeWeekends": false,
            "excludeHolidays": false,
            "dueDateChange": null
        },
        "priceComponentTagVariablesInfo": [],
        "templateId": envVariables.penalty_document_template,
        "emailTemplateId": envVariables.penalty_email_template
    }
}