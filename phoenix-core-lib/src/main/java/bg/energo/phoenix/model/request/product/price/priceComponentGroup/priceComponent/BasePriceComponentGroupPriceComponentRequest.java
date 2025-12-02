package bg.energo.phoenix.model.request.product.price.priceComponentGroup.priceComponent;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class BasePriceComponentGroupPriceComponentRequest {

    @NotNull(message = "priceComponentsList.priceComponentId-Price component ID must not be null;")
    private Long priceComponentId;

}
