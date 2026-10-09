import { randomGens } from "../../../../utils/randomGens";

type PriceParameter = {
    name: string;
    periodType: string;
    timeZone: string;
    priceParameterDetails: string[];
}

export function priceParameter(): PriceParameter {
    return{
        "name": randomGens.generateRandomString(true, false, 10),
        "periodType": "ONE_DAY",
        "timeZone": "EET",
        "priceParameterDetails": []
    }
}