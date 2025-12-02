package bg.energo.phoenix.model.enums.product.product;

import lombok.Getter;

public enum ProductSubObjectStatus {
    ACTIVE("ACTIVE"),
    DELETED("DELETED");

    @Getter
    private final String value;

    ProductSubObjectStatus(String value) {
        this.value = value;
    }
}
