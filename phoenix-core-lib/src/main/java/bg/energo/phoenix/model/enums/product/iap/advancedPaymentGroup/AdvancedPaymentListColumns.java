package bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup;

import lombok.Getter;

/**
 * <h1>AdvancedPaymentListColumns</h1>
 * {@link #ID} sort result by ID
 * {@link #NAME} sort result by NAME
 * {@link #NUMBER_OF_ADVANCED_PAYMENTS} sort result by NUMBER_OF_ADVANCED_PAYMENTS
 * {@link #DATE_OF_CREATION} sort result by DATE_OF_CREATION
 */
public enum AdvancedPaymentListColumns {
    ID("id"),
    NAME("name"),
    NUMBER_OF_ADVANCED_PAYMENTS("numberOfAdvancedPayments"),
    DATE_OF_CREATION("dateOfCreation");

    @Getter
    private String value;
    AdvancedPaymentListColumns(String value) {
        this.value = value;
    }
}
