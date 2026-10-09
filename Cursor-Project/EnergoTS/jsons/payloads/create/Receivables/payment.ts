import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type Payment = {
    paymentDate: string | number;
    initialAmount: number;
    currencyId: number;
    paymentPurpose: string | null;
    blockedForOffsetting: boolean;
    collectionChannelId: string | number | null;
    paymentPackageId: string | number | null;
    accountPeriodId: string | number;
    paymentInfo: string | null;
    customerId: string | number | null;
    contractBillingGroupId: string | number | null;
    offsettingResponseList: any;
    outgoingDocumentType: string | null;
    invoiceId: string | number | null;
    latePaymentFineId: string | number | null;
    customerDepositId: string | number | null;
    penaltyId: string | number | null;
};

export function payment(): Payment {
    return {
        "paymentDate": randomGens.generateTodaysDate("yyyy-mm-dd"),
        "initialAmount": randomGens.getRandomEven(),
        "currencyId": envVariables.currency,
        "paymentPurpose": null,
        "blockedForOffsetting": false,
        "collectionChannelId": null,
        "paymentPackageId": null,
        "accountPeriodId": envVariables.accounting_period,
        "paymentInfo": null,
        "customerId": null,
        "contractBillingGroupId": null,
        "offsettingResponseList": null,
        "outgoingDocumentType": null,
        "invoiceId": null,
        "latePaymentFineId": null,
        "customerDepositId": null,
        "penaltyId": null
    }
}