

type TemplateType = 'DOCUMENT' | 'EMAIL';
type TemplateStatus = 'ACTIVE' | 'INACTIVE';
type TemplatePurpose = 'OBJECTION_CHANGE_COORD' | string;
type Language = 'BULGARIAN' | 'ENGLISH';
type OutputFileFormat = 'XLSX' | 'PDF' | 'DOCX';
type FileName = 'DOCUMENT_NUMBER' | 'TIMESTAMP' | string;

interface CbgEmailTemplate {
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

export function cbgEmailTemplate(): CbgEmailTemplate {
    return {
        "name": "(playwright) CBG EMAIL TEMPLATE",
        "templateType": "EMAIL",
        "templateStatus": "ACTIVE",
        "templatePurpose": "OBJECTION_CHANGE_COORD",
        "defaultGoodsOrderDocument": false,
        "defaultGoodsOrderEmail": false,
        "defaultLatePaymentFineDocument": false,
        "defaultLatePaymentFineEmail": false,
        "defaultClaimedPenaltyDocument": false,
        "defaultClaimedPenaltyEmail": false,
        "language": "BULGARIAN",
        "subject": 'PLAYWRIGHT AUTOMATION',
        "quantity": null,
        "customerTypes": [],
        "consumptionPurposes": [],
        "outputFileFormat": [],
        "fileNames": [],
        "fileNamePrefix": null,
        "fileNameSuffix": null,
        "fileSignings": [],
        "fileId": null
    }
}