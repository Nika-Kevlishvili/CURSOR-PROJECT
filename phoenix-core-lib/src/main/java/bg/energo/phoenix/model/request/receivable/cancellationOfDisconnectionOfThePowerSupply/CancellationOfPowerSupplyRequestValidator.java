package bg.energo.phoenix.model.request.receivable.cancellationOfDisconnectionOfThePowerSupply;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = CancellationOfPowerSupplyRequestValidator.DisconnectionOfPowerSupplyRequestValidatorImpl.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CancellationOfPowerSupplyRequestValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class DisconnectionOfPowerSupplyRequestValidatorImpl
            implements ConstraintValidator<CancellationOfPowerSupplyRequestValidator, CancellationOfThePowerSupplyRequest> {

        @Override
        public boolean isValid(CancellationOfThePowerSupplyRequest request, ConstraintValidatorContext context) {
            StringBuilder errors = new StringBuilder();

            if (request.getTable().isEmpty()) {
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