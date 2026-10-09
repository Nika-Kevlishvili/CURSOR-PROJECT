import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type CommunicationChannel = "EMAIL" | "SMS" | "ON_PAPER" | "EMAIL_SMS_ON_PAPER";

type reminderForDisconnectionType = {
    customerSendToDateAndTime: string;
    liabilityAmountFrom: number | null;
    liabilityAmountTo: number | null;
    liabilitiesMaxDueDate: string;
    customerList: string;
    customerFilterType: string;
    communicationChannels: CommunicationChannel[];
    emailTemplateId: number | null;
    smsTemplateId: number | null;
    documentTemplateId: number | null;
    disconnectionDate: string;
    printedFileName: string;
};


export function reminderForDisconnection(): reminderForDisconnectionType {
    return {
        "customerSendToDateAndTime": randomGens.generateISOTimestampWithOffset(1, 10),
        "liabilityAmountFrom": null,
        "liabilityAmountTo": null,
        "liabilitiesMaxDueDate": randomGens.generateYesterdaysDate('yyyy-mm-dd'),
        "customerList": "",
        "customerFilterType": "INCLUDED",
        "communicationChannels": [
            "EMAIL",
            "SMS"
        ],
        "emailTemplateId": envVariables.reminder_disconnection_email_template,
        "smsTemplateId": envVariables.reminder_disconnection_sms_template,
        "documentTemplateId": null,
        "disconnectionDate": randomGens.generateTodaysDate('yyyy-mm-dd'),
        "printedFileName": "1"
    }
}