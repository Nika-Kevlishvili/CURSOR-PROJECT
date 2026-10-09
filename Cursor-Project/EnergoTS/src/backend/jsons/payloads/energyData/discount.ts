import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type Discount = {
    dateFrom: string;
    dateTo: string;
    orderNumber: string;
    certificationNumber: string;
    amountOfDiscount: string;
    amountOfDiscountInMoneyPerKWH: string;
    currencyId: string | number;
    volumeWithoutDiscountInKWH: number | null;
    customerId: string | number | null;
    pointOfDeliveryIds: (string | number | null)[];
};

export function discount(): Discount {
    return {
        "dateFrom": randomGens.generateMonthStartDate('yyyy-mm-dd'),
        "dateTo": randomGens.generateMonthEndDate('yyyy-mm-dd'),
        "orderNumber": "1",
        "certificationNumber": "1",
        "amountOfDiscount": "50",
        "amountOfDiscountInMoneyPerKWH": "10",
        "currencyId": envVariables.currency,
        "volumeWithoutDiscountInKWH": null,
        "customerId": null,
        "pointOfDeliveryIds": [
            null
        ]
    }
}