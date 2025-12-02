package bg.energo.phoenix.model.response.priceComponent;

import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailablePriceComponentResponse {

    private Long id;
    private String name;
    private String fullName;

    public static AvailablePriceComponentResponse responseFromEntity(PriceComponent priceComponent) {
        String fullName = "%s (%s)".formatted(priceComponent.getName(), priceComponent.getId());
        return AvailablePriceComponentResponse.builder()
                .id(priceComponent.getId())
                .name(priceComponent.getName())
                .fullName(fullName)
                .build();

    }

}
