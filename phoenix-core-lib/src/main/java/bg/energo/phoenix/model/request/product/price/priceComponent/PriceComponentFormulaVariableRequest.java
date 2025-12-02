package bg.energo.phoenix.model.request.product.price.priceComponent;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PriceComponentFormulaVariableRequest {

    private String variable;

    private String description;

    @DecimalMin(value = "0", message = "formulaRequest.variables.value-Minimum value is {value};")
    @DecimalMax(value = "999999999.99999", message = "formulaRequest.variables.value-Maximum value is {value};")
    @Digits(integer = 9, fraction = 5, message = "formulaRequest.variables.value-[Value] should not exceed {integer} integral and {fraction} fractional parts;")
    private BigDecimal value;

    @DecimalMin(value = "0", message = "formulaRequest.variables.valueFrom-Minimum valueFrom is {value};")
    @DecimalMax(value = "999999999.99999", message = "formulaRequest.variables.valueFrom-Maximum valueFrom is {value};")
    @Digits(integer = 9, fraction = 5, message = "formulaRequest.variables.valueFrom-[ValueFrom] should not exceed {integer} integral and {fraction} fractional parts;")
    private BigDecimal valueFrom;

    @DecimalMin(value = "0", message = "formulaRequest.variables.valueTo-Minimum valueTo is {value};")
    @DecimalMax(value = "999999999.99999", message = "formulaRequest.variables.valueTo-Maximum valueTo is {value};")
    @Digits(integer = 9, fraction = 5, message = "formulaRequest.variables.valueTo-[ValueTo] should not exceed {integer} integral and {fraction} fractional parts;")
    private BigDecimal valueTo;

    private Long balancingProfileNameId;
}
