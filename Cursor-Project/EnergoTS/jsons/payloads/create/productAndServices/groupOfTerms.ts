import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type GroupOfTermsPayload = {
    name: string;
    termsId: any[] | null;
    updateExistingVersion: boolean;
}

export function groupOfTerms(): GroupOfTermsPayload {
    return { 
        "name": randomGens.generateRandomString(),
        "termsId": null,
        "updateExistingVersion": false
    }
}