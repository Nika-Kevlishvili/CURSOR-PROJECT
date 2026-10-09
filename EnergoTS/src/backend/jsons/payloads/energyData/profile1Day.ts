import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type Profile1Day = {
    identifier: string | number | null;
    periodType: string;
    timeZone: string;
    periodFrom: string;
    periodTo: string;
    warningAcceptedByUser: boolean;
    entries: any[];
    profileId: string | number;
};

export const profile1Day: Profile1Day = {
    "identifier": null,
    "periodType": "ONE_DAY",
    "timeZone": "CET",
    "periodFrom": `${randomGens.generateMonthStartDate('yyyy-mm-dd')}T00:00:00.000Z`,
    "periodTo": `${randomGens.generateMonthEndDate('yyyy-mm-dd')}T00:00:00.000Z`,
    "warningAcceptedByUser": true,
    "entries": [],
    "profileId": envVariables.profiles
}