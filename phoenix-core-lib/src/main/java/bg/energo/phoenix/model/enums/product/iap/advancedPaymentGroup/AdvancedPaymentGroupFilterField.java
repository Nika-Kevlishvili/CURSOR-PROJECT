package bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup;

import lombok.Getter;

/**
 * <h1>AdvancedPaymentGroupFilterField</h1>
 * {@link #ALL} search in all fields
 * {@link #GROUP_OF_ADVANCED_PAYMENT_NAME} search in GROUP_OF_ADVANCED_PAYMENT_NAME field
 * {@link #ADVANCED_PAYMENT_NAME} search in ADVANCED_PAYMENT_NAME field
 */
public enum AdvancedPaymentGroupFilterField {
    ALL("ALL"),
    GROUP_OF_ADVANCED_PAYMENT_NAME("GROUP_OF_ADVANCED_PAYMENT_NAME"),
    ADVANCED_PAYMENT_NAME("ADVANCED_PAYMENT_NAME");
    @Getter
    private final String value;

    AdvancedPaymentGroupFilterField(String value) {
        this.value = value;
    }
}
