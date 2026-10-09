import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";



export function cancelationOfRequestForDisconnection(){
    return {
       "requestForDisconnectionOfThePowerSupplyId":null,
       "saveAs":"DRAFT",
       "fileIds":[],
       "table":[
          {
            "customerId": null,
            "podId": null,
            "requestForDisconnectionOfPowerSupplyId": null,
            "cancellationReasonId": envVariables.reason_for_cancellation
        }
       ],
       "templateIds":[]
    }
}