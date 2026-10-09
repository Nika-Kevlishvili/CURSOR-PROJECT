

type TemplateType = 'DOCUMENT' | 'EMAIL';
type TemplateStatus = 'ACTIVE' | 'INACTIVE';
type TemplatePurpose = 'OBJECTION_CHANGE_COORD' | string;
type Language = 'BULGARIAN' | 'ENGLISH';
type OutputFileFormat = 'XLSX' | 'PDF' | 'DOCX';
type FileName = 'DOCUMENT_NUMBER' | 'TIMESTAMP' | string;

interface CbgDocumentTemplate {
    name: string;
    templateType: TemplateType;
    templateStatus: TemplateStatus;
    templatePurpose: TemplatePurpose;
    defaultGoodsOrderDocument: boolean;
    defaultGoodsOrderEmail: boolean;
    defaultLatePaymentFineDocument: boolean;
    defaultLatePaymentFineEmail: boolean;
    defaultClaimedPenaltyDocument: boolean;
    defaultClaimedPenaltyEmail: boolean;
    language: Language;
    subject: string | null;
    quantity: number | null;
    customerTypes: string[];
    consumptionPurposes: string[];
    outputFileFormat: OutputFileFormat[];
    fileNames: FileName[];
    fileNamePrefix: string | null;
    fileNameSuffix: string | null;
    fileSignings: string[];
    fileId: string | null;
}

export function cbgDocumentTemplate(): CbgDocumentTemplate {
    return {
        "name": "(playwright) CBG DOCUMENT TEMPLATE",
        "templateType": "DOCUMENT",
        "templateStatus": "ACTIVE",
        "templatePurpose": "OBJECTION_CHANGE_COORD",
        "defaultGoodsOrderDocument": false,
        "defaultGoodsOrderEmail": false,
        "defaultLatePaymentFineDocument": false,
        "defaultLatePaymentFineEmail": false,
        "defaultClaimedPenaltyDocument": false,
        "defaultClaimedPenaltyEmail": false,
        "language": "BULGARIAN",
        "subject": null,
        "quantity": null,
        "customerTypes": [],
        "consumptionPurposes": [],
        "outputFileFormat": [
            "XLSX"
        ],
        "fileNames": [
            "DOCUMENT_NUMBER",
            "TIMESTAMP"
        ],
        "fileNamePrefix": null,
        "fileNameSuffix": null,
        "fileSignings": [],
        "fileId": null
    }
}