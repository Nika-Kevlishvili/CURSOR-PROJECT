package bg.energo.phoenix.model.enums.product.product;

import lombok.Getter;

public enum ProductStatus {
    ACTIVE("ACTIVE"),
    DELETED("DELETED");

    @Getter
    private final String value;

    ProductStatus(String value) {
        this.value = value;
    }
}
