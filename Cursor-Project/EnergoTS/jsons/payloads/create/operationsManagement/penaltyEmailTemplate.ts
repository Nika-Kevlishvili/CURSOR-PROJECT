import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

// -------------------- Type Definitions --------------------
type TemplateType = 'DOCUMENT' | 'EMAIL';
type TemplateStatus = 'ACTIVE' | 'INACTIVE';
type TemplatePurpose = 'INVOICE' | 'PRODUCT' | 'SERVICE' | 'TERMINATION' | 'PENALTY' | 'INVOICE_CANCEL' | 'DEPOSIT' | 'REMINDER_FOR_DISCONNECTION' | 'REMINDER';
type Language = 'BULGARIAN' | 'BILINGUAL' | 'ENGLISH';
type OutputFileFormat = 'PDF' | 'DOCX' | 'XLSX';
type FileName = 'CUSTOMER_IDENTIFIER' | 'CUSTOMER_NAME' | 'CUSTOMER_NUMBER' | 'DOCUMENT_NUMBER' | 'FILE_ID' | 'TIMESTAMP';
type FileSigning = 'SIGNING_WITH_SYSTEM_CERTIFICATE';

interface PenaltyEmailTemplatePayload {
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

export function penaltyEmailTemplate(): PenaltyEmailTemplatePayload {
    return {
        "name": `(playwright) penalty email template`,
        "templateType": "EMAIL",
        "templateStatus": "ACTIVE",
        "templatePurpose": "PENALTY",
        "defaultGoodsOrderDocument": null,
        "defaultGoodsOrderEmail": null,
        "defaultLatePaymentFineDocument": null,
        "defaultLatePaymentFineEmail": null,
        "language": "BULGARIAN",
        "subject": "Penalty Email Subject",
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