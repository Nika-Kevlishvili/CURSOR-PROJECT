package bg.energo.phoenix.model.request.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.customAnotations.receivable.massOperationForBlocking.ExclusionByAmountValidator;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ExclusionByAmountValidator
public class ExclusionByAmountRequest {
    @DecimalMin(value = "0", message = "exclusionByAmount.lessThan-lessThan minimum value is {value};")
    @DecimalMax(value = "9999999999.99", message = "exclusionByAmount.lessThan-lessThan maximum value is {value};")
    @Digits(integer = 10, fraction = 2, message = "exclusionByAmount-lessThan-lessThan Invalid format. lessThan should be in range 0 - 9999999999.99;")
    private BigDecimal lessThan;

    @DecimalMin(value = "0", message = "exclusionByAmount.greaterThan-greaterThan minimum value is {value};")
    @DecimalMax(value = "9999999999.99", message = "exclusionByAmount.greaterThan-greaterThan maximum value is {value};")
    @Digits(integer = 10, fraction = 2, message = "exclusionByAmount-greaterThan-greaterThan Invalid format. greaterThan should be in range 0 - 9999999999.99;")
    private BigDecimal greaterThan;

    private Long currency;
}

