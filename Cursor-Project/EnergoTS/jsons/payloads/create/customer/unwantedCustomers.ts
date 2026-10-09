import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type UnwantedCustomerPayload = {
    identificationNumber: number | string;
    name: string;
    unwantedCustomerReasonId: number;
    additionalInfo: string | null;
    contractCreateRestriction: boolean;
    orderCreateRestriction: boolean;
}

export function unwanted_customer(): UnwantedCustomerPayload {
    return {
        "identificationNumber": 909,
        "name": randomGens.generateRandomString(true),
        "unwantedCustomerReasonId": envVariables.unwanted_customer_reasons,
        "additionalInfo": null,
        "contractCreateRestriction": true,
        "orderCreateRestriction": true
    }
}