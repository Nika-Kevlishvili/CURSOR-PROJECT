import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type CustomerLiability = {
    create: string | null;
    accountingPeriodId: string | number;
    dueDate: string;
    applicableInterestRateId: string | number;
    occurrenceDate: string;
    initialAmount: number;
    currentAmount: number | null;
    currencyId: string | number;
    bankId: string | number;
    bic: string;
    customerId: null | number;
    billingGroupId: number | null;
    outgoingDocumentFromExternalSystem: string | null;
};

export function customer_liability(): CustomerLiability {
    return {
        "create": null,
        "accountingPeriodId": envVariables.accounting_period,
        "dueDate": randomGens.generateTodaysDate("dd-mm-yyyy"),
        "applicableInterestRateId": envVariables.interest_rate,
        "occurrenceDate": randomGens.generateTodaysDate("dd-mm-yyyy"),
        "initialAmount": randomGens.getRandomEven(),
        "currentAmount": null,
        "currencyId": envVariables.currency,
        "bankId": envVariables.banks,
        "bic": "RP",
        "customerId": null,
        "billingGroupId": null,
        "outgoingDocumentFromExternalSystem": null
    }
}