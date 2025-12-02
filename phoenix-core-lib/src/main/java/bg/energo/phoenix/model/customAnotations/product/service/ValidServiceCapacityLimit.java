package bg.energo.phoenix.model.customAnotations.product.service;

import bg.energo.phoenix.model.request.product.service.ServiceBasicSettingsRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidServiceCapacityLimit.ServiceCapacityLimitValidator.class})
public @interface ValidServiceCapacityLimit {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ServiceCapacityLimitValidator implements ConstraintValidator<ValidServiceCapacityLimit, ServiceBasicSettingsRequest> {

        @Override
        public boolean isValid(ServiceBasicSettingsRequest request, ConstraintValidatorContext context) {
            StringBuilder validationMessage = new StringBuilder();

            if (request.getCapacityLimitAmount() != null && request.getCapacityLimitType() == null) {
                validationMessage.append("basicSettings.capacityLimitType-Capacity Limit Type should not be null if capacity limit amount is defined;");
            }

            if (request.getCapacityLimitType() != null && request.getCapacityLimitAmount() == null) {
                validationMessage.append("capacityLimitAmount-Capacity Limit Amount should not be null if capacityLimitType is defined;");
            }

            boolean isValid = validationMessage.isEmpty();
            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessage.toString()).addConstraintViolation();
            }

            return isValid;
        }
    }
}
