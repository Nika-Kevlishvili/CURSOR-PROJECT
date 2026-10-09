import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type paymentReverse = {
  paymentId: number;
  customerId: number;
  blockedForOffsetting: boolean;
  blockedForOffsettingFromDate: Date | null;
  blockedForOffsettingToDate: Date | null;
  reasonId: number | null;
}

export function paymentReverse(): paymentReverse {
    return {
        "paymentId": 1,
        "customerId": 2,
        "blockedForOffsetting": false,
        "blockedForOffsettingFromDate": null,
        "blockedForOffsettingToDate": null,
        "reasonId": null
    }
}