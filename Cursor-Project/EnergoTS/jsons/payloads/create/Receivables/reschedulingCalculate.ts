import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type ReschedulingInterestType = "INTEREST_WITH_EVERY_INSTALLMENT" | "INTEREST_WITH_THE_FIRST_INSTALLMENT" | "INTEREST_WITH_LAST_INSTALLMENT" | "FIRST_INSTALLMENT_INTEREST_ONLY";

type ReschedulingCalculate = {
    reschedulingInterestType: ReschedulingInterestType;
    installmentAmount: number | null;
    installmentCount: string | number | null;
    installmentCurrencyId: string | number | null;
    installmentDate: string | number;
    replaceInstallmentRateId: string | number | null;
    liabilityIds: (string | number | null)[];
};

export const rescheduling_calculate: ReschedulingCalculate = {
    "reschedulingInterestType": "INTEREST_WITH_LAST_INSTALLMENT",
    "installmentAmount": null,
    "installmentCount": "2",
    "installmentCurrencyId": envVariables.currency,
    "installmentDate": randomGens.generateCurrentTimeStamp(),
    "replaceInstallmentRateId": null,
    "liabilityIds": [
        null
    ]
}