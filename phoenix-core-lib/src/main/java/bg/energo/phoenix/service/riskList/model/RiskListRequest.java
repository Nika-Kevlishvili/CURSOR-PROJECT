package bg.energo.phoenix.service.riskList.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskListRequest {

    // customer UIC/PN
    @NotBlank(message = "identifier-Identifier is mandatory;")
    private String identifier;

    @NotNull(message = "version-Customer version is mandatory;")
    private Long version;

    // these validations match the validations on the [estimatedTotalConsumptionUnderContractKwh] field in contract request
    @NotNull(message = "consumption-Consumption is mandatory;")
    @DecimalMin(value = "0.000", message = "consumption-Minimum value is {value};")
    @DecimalMax(value = "99999999.999", message = "consumption-Maximum value is {value};")
    @Digits(integer = 8, fraction = 3, message = "consumption-[Value] should not exceed {integer} integral and {fraction} fractional parts;")
    private BigDecimal consumption;

}
