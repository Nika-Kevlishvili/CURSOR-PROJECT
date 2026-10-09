import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";


type CustomerReceivable = {
    creation: string | null;
    accountingPeriodId: string | number;
    dueDate: string;
    occurrenceDate: string;
    initialAmount: number;
    currencyId: string | number;
    customerId: number | null;
    reasonId: string | number | null;
    bankId: string | number;
    bankAccount: string | null;
    blockedForOffsetting: boolean;
    blockedFromDate: string | null;
    additionalInformation: string | null;
    blockedToDate: string | null;
    billingGroupId: null | number;
};

export function customer_receivable(): CustomerReceivable {    
    return {
        "creation": null,
        "accountingPeriodId": envVariables.accounting_period,
        "dueDate": randomGens.generateTodaysDate("dd-mm-yyyy"),
        "occurrenceDate": randomGens.generateTodaysDate("dd-mm-yyyy"),
        "initialAmount": randomGens.getRandomEven(),
        "currencyId": envVariables.currency,
        "customerId": null,
        "reasonId": null,
        "bankId": envVariables.banks,
        "bankAccount": null,
        "blockedForOffsetting": false,
        "blockedFromDate": null,
        "additionalInformation": null,
        "blockedToDate": null,
        "billingGroupId": null
    }
}