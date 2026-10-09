import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

export function Cbgpayload(){
  return {
    "number": null,
    "creationDate": randomGens.generateTodaysDate(),
    "changeOfCbgStatus": "DRAFT",
    "gridOperatorId": envVariables.grid_operator,
    "dateOfChange": randomGens.generateTodaysDate('yyyy-mm-dd'),
    "invoiceFile": null,
    "fileId": null,
    "templateIds": [
        envVariables.cbg_document_template
    ],
    "emailTemplateId": envVariables.cbg_email_template,
    "saveAs": "SAVE_AS_DRAFT",
    "subFileIds": []
  } 
}