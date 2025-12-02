package bg.energo.phoenix.model.enums.product.price.priceComponentGroup;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PriceComponentGroupSearchField {
    ALL("ALL"),
    PRICE_COMPONENT_GROUP_NAME("PRICE_COMPONENT_GROUP_NAME"),
    PRICE_COMPONENT_NAME("PRICE_COMPONENT_NAME");

    private final String value;
}
