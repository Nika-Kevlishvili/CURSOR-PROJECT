package bg.energo.phoenix.model.request.product.price.priceComponent;

import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentMathVariableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceComponentProjectionForIap {

    private Long iapId;

    private Long id;

    private Long priceComponentId;

    private String priceComponentName;

    private PriceComponentMathVariableName variableName;

    private String description;

    private BigDecimal value;

    private BigDecimal valueFrom;

    private BigDecimal valueTo;

    private Long balancingProductNameId;
}
