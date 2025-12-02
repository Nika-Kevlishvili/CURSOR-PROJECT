package bg.energo.phoenix.model.enums.product.product;

import lombok.Getter;

public enum ProductDetailStatus {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");
    @Getter
    private final String value;

    ProductDetailStatus(String value) {
        this.value = value;
    }
}
