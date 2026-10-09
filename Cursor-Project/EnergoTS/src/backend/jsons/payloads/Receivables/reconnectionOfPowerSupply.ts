import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";


type reconnectionOfPowerSupplyTypes = {
    gridOperatorId: number;
    fileIds: number[];
    saveAs: "EXECUTED" | "DRAFT" | "SAVE";
    table: any;
    templateIds: number[];
};

export function reconnectionOfPowerSupply(): reconnectionOfPowerSupplyTypes {
    return {
        "gridOperatorId": envVariables.grid_operator,
        "fileIds": [],
        "saveAs": "EXECUTED",
        "table": [
            {
                "customerId": 6046506,
                "podId": 299776,
                "requestForDisconnectionOfPowerSupplyId": 1354,
                "cancellationReasonId": envVariables.reason_for_cancellation
            }
        ],
        "templateIds": []
    }
}