package bg.energo.phoenix.model.request.product.price.priceComponentGroup.priceComponent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EditPriceComponentGroupPriceComponentRequest extends BasePriceComponentGroupPriceComponentRequest {

    private Long id;

}
