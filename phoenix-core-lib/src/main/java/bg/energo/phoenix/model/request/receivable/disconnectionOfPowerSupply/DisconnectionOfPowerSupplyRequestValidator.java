package bg.energo.phoenix.model.request.receivable.disconnectionOfPowerSupply;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = DisconnectionOfPowerSupplyRequestValidator.DisconnectionOfPowerSupplyRequestValidatorImpl.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DisconnectionOfPowerSupplyRequestValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class DisconnectionOfPowerSupplyRequestValidatorImpl
            implements ConstraintValidator<DisconnectionOfPowerSupplyRequestValidator, DisconnectionOfPowerSupplyRequest> {

        @Override
        public boolean isValid(DisconnectionOfPowerSupplyRequest request, ConstraintValidatorContext context) {
            StringBuilder errors = new StringBuilder();

            if (request.disconnectedRequest() == null || request.disconnectedRequest().isEmpty()) {
                errors.append("disconnectedRequest-disconnectedRequest must not be empty.");
            }

            if (!errors.isEmpty()) {
                context.buildConstraintViolationWithTemplate(errors.toString()).addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}