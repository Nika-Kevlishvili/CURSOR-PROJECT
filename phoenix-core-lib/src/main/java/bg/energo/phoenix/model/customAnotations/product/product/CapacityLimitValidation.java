package bg.energo.phoenix.model.customAnotations.product.product;

import bg.energo.phoenix.model.request.product.product.BaseProductRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {CapacityLimitValidation.CapacityLimitValidationImpl.class})
public @interface CapacityLimitValidation {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class CapacityLimitValidationImpl implements ConstraintValidator<CapacityLimitValidation, BaseProductRequest> {
        @Override
        public boolean isValid(BaseProductRequest request, ConstraintValidatorContext constraintValidatorContext) {
            boolean isValid = true;
            if (Objects.nonNull(request.getCapacityLimitAmount())) {
                if (Objects.isNull(request.getCapacityLimitType())) {
                    isValid = false;
                    constraintValidatorContext
                            .buildConstraintViolationWithTemplate("additionalSettings.capacityLimitType-Capacity Limit Type should not be null if capacityLimitAmount is defined;")
                            .addConstraintViolation();
                }
            }
            return isValid;
        }
    }
}
