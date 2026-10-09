import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

export type BillingGroupPayload = {
    id: number | null;
    groupNumber: number | null;
    sendingInvoice: string;
    separateInvoiceForEachPod: boolean;
    directDebit: boolean;
    bankId: number | null;
    iban: string | null;
    bic: string | null;
    alternativeRecipientCustomerDetailId: number | null;
    billingCustomerCommunicationId: number | null;
    contractId: string;
}

export function billingGroup(): BillingGroupPayload {
    return{
        "id": null,
        "groupNumber": null,
        "sendingInvoice": "EMAIL",
        "separateInvoiceForEachPod": false,
        "directDebit": false,
        "bankId": null,
        "iban": null,
        "bic": null,
        "alternativeRecipientCustomerDetailId": null,
        "billingCustomerCommunicationId": null,
        "contractId": ""
    }
}