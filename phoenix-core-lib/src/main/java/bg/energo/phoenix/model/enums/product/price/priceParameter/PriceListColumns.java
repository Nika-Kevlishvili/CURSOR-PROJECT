package bg.energo.phoenix.model.enums.product.price.priceParameter;

import lombok.Getter;

public enum PriceListColumns {

    ID("id"),
    NAME("pd2.name"),
    TYPE("periodType"),
    DATE_OF_CREATION("createDate");

    @Getter
    private String value;
    PriceListColumns(String value) {
        this.value = value;
    }
}
