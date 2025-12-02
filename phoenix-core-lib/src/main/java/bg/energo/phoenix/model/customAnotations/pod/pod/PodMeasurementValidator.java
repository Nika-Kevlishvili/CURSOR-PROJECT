package bg.energo.phoenix.model.customAnotations.pod.pod;

import bg.energo.phoenix.model.request.pod.pod.PodBaseRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.BooleanUtils;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {PodMeasurementValidator.PodMeasurementValidatorImpl.class})
public @interface PodMeasurementValidator {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PodMeasurementValidatorImpl implements ConstraintValidator<PodMeasurementValidator, PodBaseRequest> {
        @Override
        public boolean isValid(PodBaseRequest podBaseRequest, ConstraintValidatorContext context) {
            StringBuilder errors = new StringBuilder();
            if (BooleanUtils.isNotTrue(podBaseRequest.getSlp()) && BooleanUtils.isNotTrue(podBaseRequest.getSettlementPeriod())) {
                errors.append("settlementPeriod-settlementPeriod or slp must be chosen;");
            }
            if (BooleanUtils.isNotTrue(podBaseRequest.getSlp()) && podBaseRequest.getMeasurementTypeId() != null) {
                errors.append("measurementTypeId-measurementTypeId must be null when settlement period is chosen;");
            }
            if (BooleanUtils.isNotTrue(podBaseRequest.getSettlementPeriod()) && podBaseRequest.getMeasurementTypeId() == null) {
                errors.append("measurementTypeId-measurementTypeId must be selected when slp period is chosen;");
            }
            if (BooleanUtils.isTrue(podBaseRequest.getSlp()) && BooleanUtils.isTrue(podBaseRequest.getSettlementPeriod())) {
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
