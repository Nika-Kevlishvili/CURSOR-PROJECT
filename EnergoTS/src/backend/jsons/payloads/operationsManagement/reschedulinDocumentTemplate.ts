import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type ReschedulingDocumentTemplate = {
  name: string;
  templateType: 'DOCUMENT'; // can extend later if needed
  templateStatus: 'ACTIVE' | 'INACTIVE';
  templatePurpose: 'RESCHEDULING';

  defaultGoodsOrderDocument: boolean;
  defaultGoodsOrderEmail: boolean;
  defaultLatePaymentFineDocument: boolean;
  defaultLatePaymentFineEmail: boolean;
  defaultClaimedPenaltyDocument: boolean;
  defaultClaimedPenaltyEmail: boolean;

  language: 'BULGARIAN' | 'ENGLISH'; // extend if needed

  subject: string | null;
  quantity: number | null;

  customerTypes: string[];
  consumptionPurposes: string[];

  outputFileFormat: ('DOCX' | 'PDF')[];
  fileNames: ('FILE_ID' | 'DOCUMENT_NUMBER' | 'TIMESTAMP')[];

  fileNamePrefix: string | null;
  fileNameSuffix: string | null;

  fileSignings: ('NO' | 'YES')[];

  fileId: number;
};

export function ReschedulingDocumentTemplate(): ReschedulingDocumentTemplate {
    return {
        "name": "(playwright) rescheduling document template",
        "templateType": "DOCUMENT",
        "templateStatus": "ACTIVE",
        "templatePurpose": "RESCHEDULING",
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
            "DOCX",
            "PDF"
        ],
        "fileNames": [
            "FILE_ID",
            "DOCUMENT_NUMBER",
            "TIMESTAMP"
        ],
        "fileNamePrefix": null,
        "fileNameSuffix": null,
        "fileSignings": [
            "NO"
        ],
        "fileId": 1510
    }
}