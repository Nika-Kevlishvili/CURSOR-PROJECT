import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type Profile15Minute = {
    identifier: string | number | null;
    periodType: string;
    timeZone: string;
    periodFrom: string;
    periodTo: string;
    warningAcceptedByUser: boolean;
    entries: any[];
    profileId: string | number;
};

export const profile15minute: Profile15Minute = {
    "identifier": null,
    "periodType": "FIFTEEN_MINUTES",
    "timeZone": "EET",
    "periodFrom": `${randomGens.generateMonthStartDate('yyyy-mm-dd')}T00:00:00.000Z`,
    "periodTo": `${randomGens.generateMonthEndDate('yyyy-mm-dd')}T00:00:00.000Z`,
    "warningAcceptedByUser": true,
    "entries": [],
    "profileId": envVariables.profiles
}