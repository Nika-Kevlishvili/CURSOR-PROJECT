package bg.energo.phoenix.model.customAnotations.receivable.collectionChannel;

import bg.energo.phoenix.model.request.receivable.collectionChannel.ExcludeLiabilitiesByAmount;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.Objects;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ExcludeLiabilitiesByAmountValidator.PodMeasurementValidatorImpl.class})
public @interface ExcludeLiabilitiesByAmountValidator {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PodMeasurementValidatorImpl implements ConstraintValidator<ExcludeLiabilitiesByAmountValidator, ExcludeLiabilitiesByAmount> {
        @Override
        public boolean isValid(ExcludeLiabilitiesByAmount excludeLiabilitiesByAmount, ConstraintValidatorContext context) {
            StringBuilder errors = new StringBuilder();
            if (Objects.nonNull(excludeLiabilitiesByAmount.getLessThan()) && Objects.nonNull(excludeLiabilitiesByAmount.getGreaterThan())) {
                if (excludeLiabilitiesByAmount.getGreaterThan().compareTo(excludeLiabilitiesByAmount.getLessThan()) < 0) {
                    errors.append("excludeLiabilitiesByAmount.lessThan-Less than must be <= Greater than;");
                }
            }

            if (!errors.isEmpty()) {
                context.buildConstraintViolationWithTemplate(errors.toString()).addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}
