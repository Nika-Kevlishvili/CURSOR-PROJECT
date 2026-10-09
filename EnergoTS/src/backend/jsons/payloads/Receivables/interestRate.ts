import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

// Types for Interest Rate payload
type InterestRatePeriod = {
    amountInPercent: number;
    baseInterestRate: number | null;
    applicableInterestRate: number;
    bir: number | null;
    fee: number | null;
    currencyId: number;
    validFrom: string;
}

type PaymentTerm = {
    id: number;
    type: string;
    name: string;
    value: number;
    valueFrom: number | null;
    valueTo: number | null;
    calendarId: number;
    calendarName: string;
    dueDateChange: unknown;
    excludes: unknown[];
    calendar: { id: number; name: string };
}

type InterestRate = {
    name: string;
    isDefault: boolean;
    type: string; // e.g. 'YEARLY'
    charging: string; // e.g. 'OVERDUE_LIABILITY'
    minAmountForInterestCharging: number | null;
    minAmountOfInterest: number | null;
    maxAmountOfInterest: number | null;
    currencyId: number;
    minAmountOfInterestInPercentOfLiability: number | null;
    maxAmountOfInterestInPercentOfTheLiability: number | null;
    gracePeriod: number | null;
    periodicity: number | null;
    grouping: boolean;
    incomeAccountNumber: string;
    costCenterControllingOrder: string;
    interestRatePeriods: InterestRatePeriod[];
    paymentTerm: PaymentTerm;
}

export function interestRate(): InterestRate {
    return {
        "name": `Interestrate${randomGens.generateCurrentTimeStamp()}`,
        "isDefault": false,
        "type": "YEARLY",
        "charging": "OVERDUE_LIABILITY",
        "minAmountForInterestCharging": null,
        "minAmountOfInterest": null,
        "maxAmountOfInterest": null,
        "currencyId": envVariables.currency,
        "minAmountOfInterestInPercentOfLiability": null,
        "maxAmountOfInterestInPercentOfTheLiability": null,
        "gracePeriod": null,
        "periodicity": null,
        "grouping": false,
        "incomeAccountNumber": "1",
        "costCenterControllingOrder": "1",
        "interestRatePeriods": [
            {
                "amountInPercent": 1,
                "baseInterestRate": null,
                "applicableInterestRate": 1,
                "bir": null,
                "fee": null,
                "currencyId": envVariables.currency,
                "validFrom": "2024-01-01"
            }
        ],
        "paymentTerm": {
            "id": 866,
            "type": "WORKING_DAYS",
            "name": "1 WORKING_DAYS ( )",
            "value": 1,
            "valueFrom": null,
            "valueTo": null,
            "calendarId": envVariables.calendar.id,
            "calendarName": "lukas calendar (don't touch)",
            "dueDateChange": null,
            "excludes": [],
            "calendar": {
                "id": envVariables.calendar.id,
                "name": "lukas calendar (don't touch)"
            }
        }
    }
}