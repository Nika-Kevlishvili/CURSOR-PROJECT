import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

// -------------------- Type Definitions --------------------
type GroupInterimPayload = {
    name: string;
    advancedPayments: (number | null)[];
}

export const group_interim: GroupInterimPayload = {
     "name": randomGens.generateRandomString(true, false, 10),
    "advancedPayments": [
        null
    ]
}