import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type PriceComponent = {
    priceComponentId: number | null;
    priceComponentName: string;
}

type GroupComponentPayload = {
    name: string;
    priceComponentsList: PriceComponent[];
}

export const groupComponent: GroupComponentPayload = {
     "name": randomGens.generateRandomString(true, false, 10),
        "priceComponentsList": [
        {
            "priceComponentId": null,
            "priceComponentName": "automated price"
        }
    ]
}