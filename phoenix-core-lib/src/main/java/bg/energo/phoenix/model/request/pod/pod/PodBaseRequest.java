package bg.energo.phoenix.model.request.pod.pod;

import bg.energo.phoenix.model.customAnotations.pod.pod.PodBlockingValidator;
import bg.energo.phoenix.model.customAnotations.pod.pod.PodMeasurementValidator;
import bg.energo.phoenix.model.enums.pod.pod.PODConsumptionPurposes;
import bg.energo.phoenix.model.enums.pod.pod.PODType;
import bg.energo.phoenix.model.enums.pod.pod.PODVoltageLevels;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;
import java.util.Set;

@Data
@PodBlockingValidator
@PodMeasurementValidator
public abstract class PodBaseRequest {
    @Size(min = 1, max = 1024, message = "name-Name size should be between {min} and {max};")
    @NotBlank(message = "name-Name can not be blank;")
    protected String name;

    @Size(min = 1, max = 33, message = "additionalIdentifier-Additional identifier size should be between {min} and {max};")
    @Pattern(regexp = "^[0-9A-Za-z]+$", message = "additionalIdentifier-Allowed symbols in additionalIdentifier are: A-Z a-z 0-9;")
    protected String additionalIdentifier;

    @Min(value = 1, message = "balancingGroupCoordinatorId-Balancing coordinator ID min value should be 1;")
    protected Long balancingGroupCoordinatorId;

    @NotNull(message = "type-Type can not be null;")
    protected PODType type;

    @NotNull(message = "estimatedMonthlyAvgConsumption-Estimated monthly average consumption can not be null;")
    @Range(min = 1, max = 999_999_99, message = "estimatedMonthlyAvgConsumption-Range should be between 1 and 999 999 99;")
    protected Integer estimatedMonthlyAvgConsumption;

    @NotNull(message = "consumptionPurpose-Consumption purpose can not be null;")
    protected PODConsumptionPurposes consumptionPurpose;

    @Min(value = 1, message = "userTypeId-User type ID min value should be 1;")
    protected Long userTypeId;

    @NotNull(message = "voltageLevel-Voltage leve can not be null;")
    protected PODVoltageLevels voltageLevel;

    @Size(min = 1, max = 13, message = "customerIdentifierByGridOperator-customerIdentifierByGridOperator should be between {min} and {max};")
    @Pattern(regexp = "^[0-9A-Za-z]+$", message = "customerIdentifierByGridOperator-Allowed symbols in customerIdentifierByGridOperator are: A-Z a-z 0-9;")
    protected String customerIdentifierByGridOperator;

    @Size(min = 1, max = 16, message = "customerNumberByGridOperator-customerNumberByGridOperator should be between {min} and {max};")
    @Pattern(regexp = "^[0-9]+$", message = "customerNumberByGridOperator-Allowed symbols in customerNumberByGridOperator are: A-Z a-z 0-9;")
    protected String customerNumberByGridOperator;

    protected Boolean settlementPeriod;
    protected Boolean slp;
    protected Long measurementTypeId;
    protected Set<Long> podAdditionalParameters;

    //    @Range(min = 1, max = 999_999_9,
    //    message = "providedPower-Provided power range should be between {min} and {max};")
    @DecimalMin(value = "0.001", message = "providedPower-Allowed min value is 0.001;")
    @DecimalMax(value = "99999999999.999", message = "providedPower-Allowed max value is 99999999999.999;")
    @Digits(integer = 11, fraction = 3,
            message = "providedPower-Value should not exceed {integer} integral and {fraction} fractional parts;")
    protected BigDecimal providedPower;

    @DecimalMin(value = "0.01", message = "multiplier-Allowed min value is 0.01;")
    @DecimalMax(value = "99999999.99", message = "multiplier-Allowed max value is 99999999.99;")
    @Digits(integer = 8, fraction = 2, message = "multiplier-Value should not exceed {integer} integral and {fraction} fractional parts;")
    protected BigDecimal multiplier;

    @Valid
    protected PodAddressRequest addressRequest;

    protected String customerIdentifier;

    protected boolean impossibleToDisconnect;
    protected boolean blockedDisconnection;
    protected boolean blockedBilling;

    protected BlockedBillingRequest blockedBillingRequest;
    protected BlockedDisconnectionRequest blockedDisconnectionRequest;
}
