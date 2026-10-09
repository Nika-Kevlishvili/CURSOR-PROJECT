import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type Compensation = {
    number: string;
    date: string;
    volumes: number;
    price: number;
    reason: string;
    documentPeriod: string;
    documentAmount: string;
    customerId: string | number | null;
    podId: string | number | null;
    recipientId: number;
    documentCurrencyId: string | number;
};

export const compensation: Compensation = {
    "number": `Automation${randomGens.generateRandomString(true, false, 10)}`,
    "date": randomGens.generateTodaysDate('yyyy-mm-dd'),
    "volumes": randomGens.getRandomEven()+randomGens.getRandomOdd(),
    "price":  randomGens.getRandomEven()+randomGens.getRandomOdd(),
    "reason": "AUTOMATION",
    "documentPeriod": randomGens.generateMonthStartDate('yyyy-mm-dd'),
    "documentAmount": "2",
    "customerId": null,
    "podId": null,
    "recipientId": 1001,
    "documentCurrencyId": envVariables.currency
}