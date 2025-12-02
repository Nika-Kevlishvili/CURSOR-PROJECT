package bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.filter;

import lombok.Getter;

public enum InterimAdvancePaymentSearchField {

    ALL("ALL"),
    NAME("NAME");

    @Getter
    private final String value;

    InterimAdvancePaymentSearchField(String value) {
        this.value = value;
    }
}
