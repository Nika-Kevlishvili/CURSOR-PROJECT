import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type TerminationItem = {
    terminationId: number | null;
    terminationName: string;
    terminationFullName: string;
}

type GroupTerminationPayload = {
    name: string;
    terminationsList: TerminationItem[];
}

export const group_termination: GroupTerminationPayload = {
    
    "name": randomGens.generateRandomString(true, false, 10),
    "terminationsList": [
        {
            "terminationId": null,
            "terminationName": randomGens.generateRandomString(true, false, 10),
            "terminationFullName": randomGens.generateRandomString(true, false, 10),
        }
    ]

}
    
