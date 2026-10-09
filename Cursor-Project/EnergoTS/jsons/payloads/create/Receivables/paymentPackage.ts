import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type PaymentPackage = {
    channelId: number | null | string,
    accountingPeriodId: string | number,
    lockStatus: string,
    paymentDate: string | number,
}

export function payment_package(): PaymentPackage {
    return {
        "channelId": null,
        "accountingPeriodId": envVariables.accounting_period,
        "lockStatus": "UNLOCKED",
        "paymentDate": randomGens.generateTodaysDate("yyyy-mm-dd"),
    }
}