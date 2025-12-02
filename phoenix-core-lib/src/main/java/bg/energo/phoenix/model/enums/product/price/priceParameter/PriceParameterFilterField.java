package bg.energo.phoenix.model.enums.product.price.priceParameter;

import lombok.Getter;

public enum PriceParameterFilterField {

    ALL("ALL"),
    NAME("NAME");

    @Getter
    private final String value;
    PriceParameterFilterField(String value) {
        this.value = value;
    }
}
