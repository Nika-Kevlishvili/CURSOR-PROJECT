package bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup;

import lombok.Getter;

/**
 * <h1>AdvancedPaymentGroupStatus</h1>
 * {@link #ACTIVE} active status
 * {@link #DELETED} deleted status
 */
public enum AdvancedPaymentGroupStatus {

    ACTIVE("ACTIVE"),
    DELETED("DELETED");
    @Getter
    private String value;

    AdvancedPaymentGroupStatus(String value) {
        this.value = value;
    }
}
