package bg.energo.phoenix.model.customAnotations.pod.pod;

import bg.energo.phoenix.model.request.pod.pod.PodContractRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.BooleanUtils;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {PodContractMeasurementValidator.PodMeasurementValidatorImpl.class})
public @interface PodContractMeasurementValidator {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PodMeasurementValidatorImpl implements ConstraintValidator<PodContractMeasurementValidator, PodContractRequest> {
        @Override
        public boolean isValid(PodContractRequest podContractRequest, ConstraintValidatorContext context) {
            StringBuilder errors = new StringBuilder();
            if (BooleanUtils.isNotTrue(podContractRequest.getSlp()) && BooleanUtils.isNotTrue(podContractRequest.getSettlementPeriod())) {
                errors.append("settlementPeriod-settlementPeriod or slp must be chosen;");
            }
            if (BooleanUtils.isNotTrue(podContractRequest.getSlp()) && podContractRequest.getMeasurementTypeId() != null) {
                errors.append("measurementTypeId-measurementTypeId must be null when settlement period is chosen;");
            }
            if (BooleanUtils.isNotTrue(podContractRequest.getSettlementPeriod()) && podContractRequest.getMeasurementTypeId() == null) {
                errors.append("measurementTypeId-measurementTypeId must be selected when slp period is chosen;");
            }
            if (BooleanUtils.isTrue(podContractRequest.getSlp()) && BooleanUtils.isTrue(podContractRequest.getSettlementPeriod())) {
                errors.append("measurementTypeId-only one of slp and SettlementPeriod can be selected;");
            }
            if (!errors.isEmpty()) {
                context.buildConstraintViolationWithTemplate(errors.toString()).addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
