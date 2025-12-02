package bg.energo.phoenix.model.response.priceComponent;

import bg.energo.phoenix.model.entity.product.price.priceComponentGroup.PriceComponentGroupDetails;
import lombok.Data;

@Data
public class PriceComponentGroupShortResponse {

    private Long id;
    private String name;

    public PriceComponentGroupShortResponse(PriceComponentGroupDetails details) {
        Long priceComponentGroupId = details.getPriceComponentGroupId();
        this.id = priceComponentGroupId;
        this.name = "%s (%s)".formatted(details.getName(), priceComponentGroupId);
    }
}
