package bg.energo.phoenix.model.request.pod.pod;

import bg.energo.phoenix.model.customAnotations.pod.pod.PodContractMeasurementValidator;
import bg.energo.phoenix.model.entity.pod.pod.PodContractResponse;
import bg.energo.phoenix.model.enums.pod.pod.PODConsumptionPurposes;
import bg.energo.phoenix.model.enums.pod.pod.PODMeasurementType;
import bg.energo.phoenix.model.enums.pod.pod.PODType;
import bg.energo.phoenix.model.enums.pod.pod.PODVoltageLevels;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

@Data
@PodContractMeasurementValidator
public class PodContractRequest {
    @NotBlank(message = "identifier-Identifier can not be blank;")
    @Size(min = 1, max = 33, message = "identifier-Identifier length should be between {min}:{max};")
    @Pattern(regexp = "^[0-9A-Za-z]+$", message = "number-Allowed symbols in number are: A-Z a-z 0-9;")
    private String identifier;

    @NotNull(message = "gridOperatorId-Grid Operator can not be null;")
    @Min(value = 1, message = "gridOperatorId-Grid Operator ID min value should be 1;")
    protected Long gridOperatorId;

    @Size(min = 1, max = 1024, message = "name-Name size should be between {min} and {max};")
    @NotBlank(message = "name-Name can not be blank;")
    protected String name;

    @NotNull(message = "type-Type can not be null;")
    protected PODType type;

    @NotNull(message = "estimatedMonthlyAvgConsumption-Estimated monthly average consumption can not be null;")
    @Range(min = 1, max = 999_999_99, message = "estimatedMonthlyAvgConsumption-Range should be between 1 and 999 999 99;")
    protected Integer estimatedMonthlyAvgConsumption;

    @NotNull(message = "consumptionPurpose-Consumption purpose can not be null;")
    protected PODConsumptionPurposes consumptionPurpose;
    @NotNull(message = "voltageLevel-Voltage leve can not be null;")
    protected PODVoltageLevels voltageLevel;

    //    @Range(min = 1, max = 999_999_9, message = "providedPower-Provided power range should be between {min} and {max};")
    @DecimalMin(value = "0.001", message = "providedPower-Allowed min value is 0.001;")
    @DecimalMax(value = "99999999999.999", message = "providedPower-Allowed max value is 99999999999.999;")
    @Digits(integer = 11, fraction = 3,
            message = "providedPower-Value should not exceed {integer} integral and {fraction} fractional parts;")
    protected BigDecimal providedPower;

    @DecimalMin(value = "0.01", message = "multiplier-Allowed min value is 0.01;")
    @DecimalMax(value = "99999999.00", message = "multiplier-Allowed max value is 99999999.00;")
    @Digits(integer = 8, fraction = 2, message = "multiplier-Value should not exceed {integer} integral and {fraction} fractional parts;")
    protected BigDecimal multiplier;

    @Valid
    protected PodAddressRequest addressRequest;

    protected Set<Long> podAdditionalParameters;

    protected Boolean settlementPeriod;
    protected Boolean slp;
    protected Long measurementTypeId;

    public boolean equalsResponse(PodContractResponse that) {

        if (!Objects.equals(getIdentifier(), that.getIdentifier())) return false;
        if (!Objects.equals(getGridOperatorId(), that.getGridOperatorId())) return false;
        if (!Objects.equals(getName(), that.getName())) return false;
        if (!Objects.equals(getType(), that.getType())) return false;
        if (!Objects.equals(getEstimatedMonthlyAvgConsumption(), that.getEstimatedMonthlyAvgConsumption()))
            return false;
        if (!Objects.equals(getConsumptionPurpose(), that.getConsumptionPurpose())) return false;
        if (!Objects.equals(getVoltageLevel(), that.getVoltageLevel())) return false;
        if (!Objects.equals(getSlp() ? PODMeasurementType.SLP : PODMeasurementType.SETTLEMENT_PERIOD, that.getMeasurementType()))
            return false;
        if (!Objects.equals(getProvidedPower(), that.getProvidedPower())) return false;
        if (!Objects.equals(getMultiplier(), that.getMultiplier())) return false;
        if (that.getPodViewMeasurementType() != null) {
            if (!Objects.equals(getMeasurementTypeId(), that.getPodViewMeasurementType().getMeasurementTypeId()))
                return false;
        }
        if (!Objects.equals(getPodAdditionalParameters(), that.getPodAdditionalParameters())) return false;
        return getAddressRequest().equalsResponse(that);
    }

}
