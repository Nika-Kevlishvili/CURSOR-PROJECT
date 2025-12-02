package bg.energo.phoenix.model.request.receivable.collectionChannel;

import bg.energo.phoenix.model.customAnotations.receivable.collectionChannel.ExcludeLiabilitiesByAmountValidator;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcludeLiabilitiesByAmountValidator
public class ExcludeLiabilitiesByAmount {

    @DecimalMin(value = "0.00", message = "excludeLiabilitiesByAmount.lessThan-Minimum value is {value};")
    @DecimalMax(value = "99999999.99", message = "excludeLiabilitiesByAmount.lessThan-Maximum value is {value};")
    @Digits(integer = 8, fraction = 2, message = "excludeLiabilitiesByAmount.lessThan-[Value] should not exceed {integer} integral and {fraction} fractional parts;")
    private BigDecimal lessThan;

    @DecimalMin(value = "0.00", message = "excludeLiabilitiesByAmount.greaterThan-Minimum value is {value};")
    @DecimalMax(value = "99999999.99", message = "excludeLiabilitiesByAmount.greaterThan-Maximum value is {value};")
    @Digits(integer = 8, fraction = 2, message = "excludeLiabilitiesByAmount.greaterThan-[Value] should not exceed {integer} integral and {fraction} fractional parts;")
    private BigDecimal greaterThan;

}
