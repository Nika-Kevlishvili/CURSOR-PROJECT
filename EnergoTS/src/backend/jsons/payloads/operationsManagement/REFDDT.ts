export type TemplateType = "DOCUMENT" | "EMAIL";

export type TemplateStatus = "ACTIVE" | "INACTIVE";

export type TemplatePurpose = 
    | "REMINDER_DISCONNECT_POWER"
    | "LATE_PAYMENT_FINE"
    | "GOODS_ORDER"
    | "PRODUCT_CONTRACT_CONCLUSION"
    | "SERVICE_CONTRACT_CONCLUSION";

export type Language = "BULGARIAN" | "ENGLISH";

export type OutputFileFormat = "PDF" | "DOCX" | "HTML";

export type FileNameComponent = 
    | "DOCUMENT_NUMBER"
    | "FILE_ID"
    | "TIMESTAMP"
    | "CUSTOMER_NAME"
    | "CUSTOMER_ID";

export type FileSigning = "NO" | "YES" | "DIGITAL";

export interface ReminderDisconnectionDocumentTemplatePayload {
    name: string;
    templateType: TemplateType;
    templateStatus: TemplateStatus;
    templatePurpose: TemplatePurpose;
    defaultGoodsOrderDocument: boolean | null;
    defaultGoodsOrderEmail: boolean | null;
    defaultLatePaymentFineDocument: boolean | null;
    defaultLatePaymentFineEmail: boolean | null;
    defaultProductContractConclusion: boolean | null;
    defaultServiceContractConclusion: boolean | null;
    language: Language;
    subject: string | null;
    quantity: number | null;
    customerTypes: string[] | null;
    consumptionPurposes: string[] | null;
    outputFileFormat: OutputFileFormat[];
    fileNames: FileNameComponent[];
    fileNamePrefix: string | null;
    fileNameSuffix: string | null;
    fileSignings: FileSigning[];
    fileId: string | null;
}

export function reminderDisconnectionDocumentTemplate(): ReminderDisconnectionDocumentTemplatePayload {
    return {
        "name": `(playwright) reminder for disconnection document template `,
        "templateType": "DOCUMENT",
        "templateStatus": "ACTIVE",
        "templatePurpose": "REMINDER_DISCONNECT_POWER",
        "defaultGoodsOrderDocument": null,
        "defaultGoodsOrderEmail": null,
        "defaultLatePaymentFineDocument": null,
        "defaultLatePaymentFineEmail": null,
        "defaultProductContractConclusion": null,
        "defaultServiceContractConclusion": null,
        "language": "BULGARIAN",
        "subject": null,
        "quantity": null,
        "customerTypes": null,
        "consumptionPurposes": null,
        "outputFileFormat": [
            "PDF"
        ],
        "fileNames": [
            "DOCUMENT_NUMBER",
            "FILE_ID",
            "TIMESTAMP"
        ],
        "fileNamePrefix": null,
        "fileNameSuffix": null,
        "fileSignings": [
            "NO"
        ],
        "fileId": null
    }
}