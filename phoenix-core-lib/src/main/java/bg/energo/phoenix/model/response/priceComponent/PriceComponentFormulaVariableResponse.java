package bg.energo.phoenix.model.response.priceComponent;

import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentMathVariableName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PriceComponentFormulaVariableResponse {
    private Long id;
    private PriceComponentMathVariableName variable;
    private String description;
    private Long value;
    private Long valueFrom;
    private Long valueTo;


}
