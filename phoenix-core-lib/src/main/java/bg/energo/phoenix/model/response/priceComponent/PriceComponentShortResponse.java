package bg.energo.phoenix.model.response.priceComponent;

import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import lombok.Data;

@Data
public class PriceComponentShortResponse {

    private Long id;
    private String name;

    public PriceComponentShortResponse(PriceComponent priceComponent) {
        this.id = priceComponent.getId();
        this.name = "%s (%s)".formatted(priceComponent.getName(), priceComponent.getId());
    }
}
