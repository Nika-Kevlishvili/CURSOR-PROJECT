package bg.energo.phoenix.model.request.contract.order.service;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceOrderFormulaVariableRequest {

    @NotNull(message = "value-value can not be null;")
    @DecimalMin(value = "0", message = "value-Minimum value is {value};")
    @DecimalMax(value = "999999999.99999", message = "value-Maximum value is {value};")
    @Digits(integer = 9, fraction = 5, message = "value-[Value] should not exceed {integer} integral and {fraction} fractional parts;")
    private BigDecimal value;

    @NotNull(message = "formulaVariableId-formulaVariableId can not be null;")
    private Long formulaVariableId;

}
