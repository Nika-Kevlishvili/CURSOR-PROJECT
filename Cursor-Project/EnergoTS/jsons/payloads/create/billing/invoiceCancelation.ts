
import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type invoiceCancellation = {
    invoices: string;
    fileId: null | number;
    taxEventDate: string;
    templateId: number;
}

export function invoiceCancellation(): invoiceCancellation
{
    return {
        
    "invoices": "909",
    "fileId": null,
    "taxEventDate": randomGens.generateTodaysDate("yyyy-mm-dd"),
    "templateId": envVariables.invoice_cancellation_template

    }
}