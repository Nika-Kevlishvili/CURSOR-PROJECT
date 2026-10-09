import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

export function requestForDisconnectionDocumentTemplate() {
    return {
        "name": `(playwright) request for disconnection document template`,
        "templateType": "DOCUMENT",
        "templateStatus": "ACTIVE",
        "templatePurpose": "REQUEST_DISCONNECT_POWER",
        "defaultGoodsOrderDocument": null,
        "defaultGoodsOrderEmail": null,
        "defaultLatePaymentFineDocument": null,
        "defaultLatePaymentFineEmail": null,
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