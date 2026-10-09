import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type GroupPenaltyPayload = {
    name: string;
    penalties: (number | null)[];
}

export const groupPenalty: GroupPenaltyPayload = {
    "name": randomGens.generateRandomString(true, false, 10),
    "penalties": [
        null
    ]
}