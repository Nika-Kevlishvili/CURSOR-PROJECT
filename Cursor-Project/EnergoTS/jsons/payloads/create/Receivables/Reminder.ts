import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type TriggerForLiabilities = "WHEN_OVERDUE" | "ON_DUE_DATE" | "WHEN_INVOICES" | "REMINDER_FOR_DISCONNECTION";
type CommunicationChannel = "EMAIL" | "SMS" | "ON_PAPER";
type ConditionType = "LIST_OF_CUSTOMERS" | "ALL_CUSTOMERS" | "CUSTOMERS_UNDER_CONDITIONS";

type Reminder = {
    number: number | null;
    triggerForLiabilities: TriggerForLiabilities | null;
    postponementInDays: number | null;
    dueAmountFrom: number | null;
    dueAmountTo: number | null;
    excludeLiabilitiesByPrefixes: string | null;
    onlyLiabilitiesWithPrefixes: string | null;
    conditionValueControl: string | null;
    communicationChannels: CommunicationChannel[];
    purposeOfTheContactId: string | number;
    periodicityIds: (string | number | null)[];
    listOfCustomers: string;
    conditionType: ConditionType;
    templateId: string | number | null;
    emailTemplateId: string | number;
    smsTemplateId: string | number | null;
    documentTemplateId: string | number | null;
    listType: "CUSTOMERS" | "CONTRACTS"
};


export function reminder(): Reminder {
    return {
        "number": null,
        "triggerForLiabilities": "ON_DUE_DATE",
        "postponementInDays": null,
        "dueAmountFrom": null,
        "dueAmountTo": null,
        "excludeLiabilitiesByPrefixes": null,
        "onlyLiabilitiesWithPrefixes": null,
        "conditionValueControl": null,
        "communicationChannels": [
            "EMAIL"
        ],
        "purposeOfTheContactId": envVariables.contact_purpose,
        "periodicityIds": [],
        "listOfCustomers": "",
        "conditionType": "LIST_OF_CUSTOMERS",
        "templateId": null,
        "emailTemplateId": envVariables.reminder_email_template,
        "smsTemplateId": null,
        "documentTemplateId": null,
        "listType": "CUSTOMERS"
    }
}