package bg.energo.phoenix.model.enums.product.price.priceComponentGroup;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PriceComponentGroupTableColumn {
    PCG_ID("groupId"),
    PCG_DATE_OF_CREATION("dateOfCreation"),
    PCG_NAME("name"),
    NUM_OF_PRICE_COMPONENTS("numberOfPriceComponents");

    private final String value;
}
