package bg.energo.phoenix.model.request.receivable.reconnectionOfPowerSupply;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = ReconnectionOfThePowerSupplyEditRequestValidator.DisconnectionOfPowerSupplyRequestValidatorImpl.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReconnectionOfThePowerSupplyEditRequestValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class DisconnectionOfPowerSupplyRequestValidatorImpl
            implements ConstraintValidator<ReconnectionOfThePowerSupplyEditRequestValidator, ReconnectionOfThePowerSupplyEditRequest> {

        @Override
        public boolean isValid(ReconnectionOfThePowerSupplyEditRequest request, ConstraintValidatorContext context) {
            StringBuilder errors = new StringBuilder();

            if (request.getExistingPodChangeRequest() == null || request.getExistingPodChangeRequest().isEmpty()) {
                errors.append("existingPodChangeRequest-existingPodChangeRequest must not be empty.");
            }

            if (!errors.isEmpty()) {
                context.buildConstraintViolationWithTemplate(errors.toString()).addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}