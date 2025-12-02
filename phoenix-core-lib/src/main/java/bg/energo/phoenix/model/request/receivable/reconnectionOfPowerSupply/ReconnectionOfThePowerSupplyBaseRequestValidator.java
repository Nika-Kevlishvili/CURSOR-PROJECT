package bg.energo.phoenix.model.request.receivable.reconnectionOfPowerSupply;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = ReconnectionOfThePowerSupplyBaseRequestValidator.DisconnectionOfPowerSupplyRequestValidatorImpl.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReconnectionOfThePowerSupplyBaseRequestValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class DisconnectionOfPowerSupplyRequestValidatorImpl
            implements ConstraintValidator<ReconnectionOfThePowerSupplyBaseRequestValidator, ReconnectionOfThePowerSupplyBaseRequest> {

        @Override
        public boolean isValid(ReconnectionOfThePowerSupplyBaseRequest request, ConstraintValidatorContext context) {
            StringBuilder errors = new StringBuilder();

            if (!(request instanceof ReconnectionOfThePowerSupplyEditRequest) && (request.getTable() == null || request.getTable().isEmpty())) {
                errors.append("table-table must not be empty.");
            }

            if (!errors.isEmpty()) {
                context.buildConstraintViolationWithTemplate(errors.toString()).addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}