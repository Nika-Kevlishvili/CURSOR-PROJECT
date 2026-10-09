import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type ReschedulingCalculation = {
  reschedulingInterestType: string;
  installmentAmount: string | null;
  installmentCount: string;
  installmentCurrencyId: number;
  installmentDate: string;
  installmentDateDay: number;
  liabilityIds: number[];
};

export function CalculateRescheduling(): ReschedulingCalculation {
    return {
        
        "reschedulingInterestType": "INTEREST_WITH_THE_FIRST_INSTALLMENT",
        "installmentAmount": null,
        "installmentCount": "3",
        "installmentCurrencyId": envVariables.currency,
        "installmentDate": randomGens.generateMonthHalfDate("yyyy-mm-dd",1),
        "installmentDateDay": 15,
        "liabilityIds": [
            56844
        ]
    }
}