import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type ClaimedPenalty = {
    dueDate: string;
    amount: string;
    incomeAccountNumber: string;
    costCenter: string;
    currencyId: number;
    customerDetailId: number | null;
    billingGroupId: number | null;
    documentTemplateId: number;
    emailTemplateId: number;
};

export function claimedPenalty(): ClaimedPenalty {
    return {
        "dueDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
        "amount": "1",
        "incomeAccountNumber": "1",
        "costCenter": "1",
        "currencyId": envVariables.currency,
        "customerDetailId": null,
        "billingGroupId": null,
        "documentTemplateId": envVariables.penalty_document_template,
        "emailTemplateId": envVariables.penalty_email_template
    }
}
