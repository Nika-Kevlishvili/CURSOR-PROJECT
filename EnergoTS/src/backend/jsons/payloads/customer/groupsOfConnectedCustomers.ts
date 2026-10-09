import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type GroupsOfConnectedCustomers = {
    groupName: string;
    connectionTypeId: number | string;
    additionalInformation: string | null;
    customerIds: number[];
}

export function groups_of_connected_customers(): GroupsOfConnectedCustomers {
    return {
        "groupName": randomGens.generateRandomString(true),
        "connectionTypeId": envVariables.type_of_connection_for_gcc,
        "additionalInformation": null,
        "customerIds": []
    };
}