type TemplateType = 'DOCUMENT' | 'EMAIL';
type TemplateStatus = 'ACTIVE' | 'INACTIVE';
type TemplatePurpose = 'INVOICE' | 'PRODUCT' | 'SERVICE' | 'TERMINATION' | 'PENALTY' | 'INVOICE_CANCEL' | 'DEPOSIT' | 'REMINDER_FOR_DISCONNECTION' | 'REMINDER' | 'EMAIL';
type Language = 'BULGARIAN' | 'BILINGUAL' | 'ENGLISH';
type OutputFileFormat = 'PDF' | 'DOCX' | 'XLSX';
type FileName = 'CUSTOMER_IDENTIFIER' | 'CUSTOMER_NAME' | 'CUSTOMER_NUMBER' | 'DOCUMENT_NUMBER' | 'FILE_ID' | 'TIMESTAMP';
type FileSigning = 'NO' | 'SIGNING_WITH_SYSTEM_CERTIFICATE';

interface EmailDocumentTemplatePayload {
    name: string;
    templateType: TemplateType;
    templateStatus: TemplateStatus;
    templatePurpose: TemplatePurpose;
    defaultGoodsOrderDocument: boolean;
    defaultGoodsOrderEmail: boolean;
    defaultLatePaymentFineDocument: boolean;
    defaultLatePaymentFineEmail: boolean;
    defaultProductContractConclusion: boolean;
    defaultServiceContractConclusion: boolean;
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

export function emailDocumentTemplate(): EmailDocumentTemplatePayload {
    return {
    "name": "(playwright) EMAIL DOCUMENT template",
    "templateType": "DOCUMENT",
    "templateStatus": "ACTIVE",
    "templatePurpose": "EMAIL",
    "defaultGoodsOrderDocument": false,
    "defaultGoodsOrderEmail": false,
    "defaultLatePaymentFineDocument": false,
    "defaultLatePaymentFineEmail": false,
    "defaultProductContractConclusion": false,
    "defaultServiceContractConclusion": false,
    "language": "BULGARIAN",
    "subject": null,
    "quantity": null,
    "customerTypes": null,
    "consumptionPurposes": null,
    "outputFileFormat": [
        "PDF"
    ],
    "fileNames": [
        "CUSTOMER_IDENTIFIER",
        "CUSTOMER_NAME",
        "CUSTOMER_NUMBER",
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