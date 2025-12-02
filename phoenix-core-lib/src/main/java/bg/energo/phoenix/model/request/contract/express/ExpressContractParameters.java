package bg.energo.phoenix.model.request.contract.express;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
@Data
public class ExpressContractParameters {
    @NotNull(message = "productId-productId is mandatory;")
    private Long productId;
    @NotNull(message = "productVersionId-productVersionId is mandatory;")
    private Long productVersionId;
    private boolean procurementLaw;
    @PastOrPresent(message = "signingDate-signingDate must be a date in the past or in the present")
    private LocalDate signingDate;

    @DecimalMin(value = "0.000", message = "additionalParameters.estimatedTotalConsumptionUnderContractKwh-Minimum value is {value};")
    @DecimalMax(value = "99999999.999", message = "additionalParameters.estimatedTotalConsumptionUnderContractKwh-Maximum value is {value};")
    @Digits(integer = 8, fraction = 3, message = "additionalParameters.estimatedTotalConsumptionUnderContractKwh-[Value] should not exceed {integer} integral and {fraction} fractional parts;")
    private BigDecimal estimatedTotalConsumption;

    private Long campaignId;

    @Valid
    private ExpressContractBankingDetails bankingDetails;
}
