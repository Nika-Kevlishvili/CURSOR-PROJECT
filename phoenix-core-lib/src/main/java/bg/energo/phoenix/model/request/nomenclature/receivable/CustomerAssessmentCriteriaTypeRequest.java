package bg.energo.phoenix.model.request.nomenclature.receivable;

import bg.energo.phoenix.model.enums.nomenclature.CustomerAssessmentCriteriaType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerAssessmentCriteriaTypeRequest {

    @NotNull(message = "criteriaType-criteriaType must not be null")
    private CustomerAssessmentCriteriaType criteriaType;

    @DecimalMin(value = "0.01", message = "valueFrom-valueFrom Minimum value is {value};")
    @DecimalMax(value = "999999999.99", message = "valueFrom-valueFrom Maximum value is {value};")
    @Digits(integer = 9, fraction = 2, message = "valueFrom-valueFrom should not exceed {integer} integral and {fraction} fractional parts;")
    private BigDecimal valueFrom;

    @DecimalMin(value = "0.01", message = "valueTo-valueTo Minimum value is {value};")
    @DecimalMax(value = "999999999.99", message = "valueTo-valueTo Maximum value is {value};")
    @Digits(integer = 9, fraction = 2, message = "valueTo-valueTo should not exceed {integer} integral and {fraction} fractional parts;")
    private BigDecimal valueTo;

    private Boolean value;

}
