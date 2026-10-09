import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

interface DisconnectedRequest {
    customerId: number | null;
    podId: number | null;
    gridOperatorTaxesId: number;
    expressReconnection: boolean;
    dateOfDisconnection: string;
}

interface DisconnectionOfPowerSupplyPayload {
    requestForDisconnectionId: number | null;
    saveType: string;
    disconnectedRequest: DisconnectedRequest[];
}

export function disconnectionOfPowerSupply(): DisconnectionOfPowerSupplyPayload {
    return {
        "requestForDisconnectionId": null,
        "saveType": "EXECUTED",
        "disconnectedRequest": [
            {
                "customerId": null,
                "podId": null,
                "gridOperatorTaxesId": envVariables.taxes_for_grid_operator,
                "expressReconnection": false,
                "dateOfDisconnection": randomGens.generateTodaysDate('yyyy-mm-dd')
            }
        ]
    }
}