package bg.energo.phoenix.model.request.contract.product.additionalParameters;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.service.riskList.model.RiskListDecision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductContractAdditionalParametersRequest {

    @Pattern(regexp = "^[0-9]*$", message = "additionalParameters.dealNumber-dealNumber must contain only digits;")
    @Size(min = 1, max = 32, message = "additionalParameters.dealNumber-dealNumber must be between {min} and {max} characters;")
    private String dealNumber;

    @NotNull(message = "additionalParameters.estimatedTotalConsumptionUnderContractKwh-Field is mandatory;")
    @DecimalMin(value = "0.000", message = "additionalParameters.estimatedTotalConsumptionUnderContractKwh-Minimum value is {value};")
    @DecimalMax(value = "99999999.999", message = "additionalParameters.estimatedTotalConsumptionUnderContractKwh-Maximum value is {value};")
    @Digits(integer = 8, fraction = 3, message = "additionalParameters.estimatedTotalConsumptionUnderContractKwh-[Value] should not exceed {integer} integral and {fraction} fractional parts;")
    private BigDecimal estimatedTotalConsumptionUnderContractKwh;

    @Valid
    @NotNull(message = "additionalParameters.bankingDetails-Banking Details are mandatory;")
    private ProductContractBankingDetails bankingDetails;

    private RiskListDecision riskAssessment;

    private List<String> riskAssessmentAdditionalConditions;

    @NotNull(message = "additionalParameters.interestRateId-Interest Rate is mandatory;")
    private Long interestRateId;

    private Long campaignId;

    private List<Long> internalIntermediaries;

    private List<Long> externalIntermediaries;

    @DuplicatedValuesValidator(fieldPath = "additionalParameters.assistingEmployees")
    private List<Long> assistingEmployees;

    private Long employeeId;

}
