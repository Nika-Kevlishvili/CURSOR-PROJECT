package bg.energo.phoenix.model.enums.product.product;

import lombok.Getter;

public enum PurposeOfConsumption {
    HOUSEHOLD("HOUSEHOLD"),
    NON_HOUSEHOLD("NON_HOUSEHOLD");

    @Getter
    private final String value;

    PurposeOfConsumption(String value) {
        this.value = value;
    }
}
