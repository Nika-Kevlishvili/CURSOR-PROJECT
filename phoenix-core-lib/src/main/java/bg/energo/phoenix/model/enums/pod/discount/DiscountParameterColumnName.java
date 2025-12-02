package bg.energo.phoenix.model.enums.pod.discount;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum DiscountParameterColumnName {
    ID("id"),
    POINT_OF_DELIVERY("dp.podIdentifier"),
    CUSTOMER_IDENTIFIER("c.identifier"),
    DATE_FROM("dateFrom"),
    DATE_TO("dateTo"),
    AMOUNT_IN_PERCENT("amountInPercent"),
    AMOUNT_IN_PERCENT_PER_KWH("amountInMoneyPerKWH"),
    DATE_OF_CREATION("createDate"),
    INVOICED("invoiced");

    @Getter
    private final String value;
}
