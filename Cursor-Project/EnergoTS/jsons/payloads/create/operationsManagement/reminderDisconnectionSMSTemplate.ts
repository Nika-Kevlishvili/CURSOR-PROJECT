import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

// -------------------- Type Definitions --------------------
type TemplateType = 'DOCUMENT' | 'EMAIL' | 'SMS';
type TemplateStatus = 'ACTIVE' | 'INACTIVE';
type TemplatePurpose = 'INVOICE' | 'PRODUCT' | 'SERVICE' | 'TERMINATION' | 'PENALTY' | 'INVOICE_CANCEL' | 'DEPOSIT' | 'REMINDER_DISCONNECT_POWER' | 'REMINDER';
type Language = 'BULGARIAN' | 'BILINGUAL' | 'ENGLISH';
type OutputFileFormat = 'PDF' | 'DOCX' | 'XLSX';
type FileName = 'CUSTOMER_IDENTIFIER' | 'CUSTOMER_NAME' | 'CUSTOMER_NUMBER' | 'DOCUMENT_NUMBER' | 'FILE_ID' | 'TIMESTAMP';
type FileSigning = 'SIGNING_WITH_SYSTEM_CERTIFICATE';

interface ReminderDisconnectionSMSTemplatePayload {
    name: string;
    templateType: TemplateType;
    templateStatus: TemplateStatus;
    templatePurpose: TemplatePurpose;
    defaultGoodsOrderDocument: boolean | null;
    defaultGoodsOrderEmail: boolean | null;
    defaultLatePaymentFineDocument: boolean | null;
    defaultLatePaymentFineEmail: boolean | null;
    language: Language;
    subject: string | null;
    quantity: number | null;
    customerTypes: string[] | null;
    consumptionPurposes: string[] | null;
    outputFileFormat: OutputFileFormat[];
    fileNames: FileName[];
    fileNamePrefix: string | null;
    fileNameSuffix: string | null;
    fileSignings: FileSigning[];
    fileId: number | null;
}

export function reminderDisconnectionSMSTemplate(): ReminderDisconnectionSMSTemplatePayload {
    return {
        "name": `(playwright) reminder disconnection SMS template`,
        "templateType": "SMS",
        "templateStatus": "ACTIVE",
        "templatePurpose": "REMINDER_DISCONNECT_POWER",
        "defaultGoodsOrderDocument": null,
        "defaultGoodsOrderEmail": null,
        "defaultLatePaymentFineDocument": null,
        "defaultLatePaymentFineEmail": null,
        "language": "BULGARIAN",
        "subject": null,
        "quantity": null,
        "customerTypes": null,
        "consumptionPurposes": null,
        "outputFileFormat": [],
        "fileNames": [],
        "fileNamePrefix": null,
        "fileNameSuffix": null,
        "fileSignings": [],
        "fileId": null
    }
}