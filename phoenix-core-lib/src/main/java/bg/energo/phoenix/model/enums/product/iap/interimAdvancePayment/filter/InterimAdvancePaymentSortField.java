package bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.filter;

import lombok.Getter;

public enum InterimAdvancePaymentSortField {

    ID("id"),
    NAME("name"),
    VALUE_TYPE("valueType"),
    DEDUCTION_FROM("deductionFrom"),
    AVAILABLE("available"),
    DATE_OF_CREATION("createDate");

    @Getter
    private final String value;

    InterimAdvancePaymentSortField(String value){this.value = value;}
}
