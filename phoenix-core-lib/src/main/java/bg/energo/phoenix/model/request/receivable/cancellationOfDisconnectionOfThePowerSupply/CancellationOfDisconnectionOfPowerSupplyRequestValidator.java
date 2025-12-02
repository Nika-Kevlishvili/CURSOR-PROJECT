package bg.energo.phoenix.model.request.receivable.cancellationOfDisconnectionOfThePowerSupply;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = CancellationOfDisconnectionOfPowerSupplyRequestValidator.DisconnectionOfPowerSupplyRequestValidatorImpl.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CancellationOfDisconnectionOfPowerSupplyRequestValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class DisconnectionOfPowerSupplyRequestValidatorImpl
            implements ConstraintValidator<CancellationOfDisconnectionOfPowerSupplyRequestValidator, CancellationOfDisconnectionOfThePowerSupplyEditRequest> {

        @Override
        public boolean isValid(CancellationOfDisconnectionOfThePowerSupplyEditRequest request, ConstraintValidatorContext context) {
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