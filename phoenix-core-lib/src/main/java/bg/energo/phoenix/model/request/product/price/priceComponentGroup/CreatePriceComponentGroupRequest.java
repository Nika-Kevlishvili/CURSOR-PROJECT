package bg.energo.phoenix.model.request.product.price.priceComponentGroup;

import bg.energo.phoenix.model.request.product.price.priceComponentGroup.priceComponent.CreatePriceComponentGroupPriceComponentRequest;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreatePriceComponentGroupRequest extends BasePriceComponentGroupRequest {

    private List<@Valid CreatePriceComponentGroupPriceComponentRequest> priceComponentsList;

}
