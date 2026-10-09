import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type ProfileEntry = {
    periodFrom: string;
    value: number;
    shiftedHour: boolean;
};

type Profile1Month = {
    identifier: string | number | null;
    periodType: string;
    timeZone: string;
    periodFrom: string;
    periodTo: string;
    warningAcceptedByUser: boolean;
    entries: ProfileEntry[];
    profileId: string | number;
};

export function profile1Month(): Profile1Month {
    return{
    "identifier": '', // from pod
    "periodType": "ONE_MONTH",
    "timeZone": "EET",
    "periodFrom": `${randomGens.generateMonthStartDate('yyyy-mm-dd')}T00:00:00.000Z`,
    "periodTo": `${randomGens.generateMonthEndDate('yyyy-mm-dd')}T00:00:00.000Z`,
    "warningAcceptedByUser": true,
    "entries": [
        {
            "periodFrom": `${randomGens.generateMonthStartDate('yyyy-mm-dd')}T00:00:00.000Z`,
            "value": randomGens.getRandomEven(),
            "shiftedHour": false
        }
    ],
    "profileId": envVariables.profiles}
}