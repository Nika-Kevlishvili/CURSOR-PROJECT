package bg.energo.phoenix.model.customAnotations.receivable.payment;

import bg.energo.phoenix.model.request.receivable.payment.CreatePaymentRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {CreatePaymentRequestValidator.CreatePaymentRequestValidatorImpl.class})
@Documented
public @interface CreatePaymentRequestValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class CreatePaymentRequestValidatorImpl implements ConstraintValidator<CreatePaymentRequestValidator, CreatePaymentRequest> {
        @Override
        public boolean isValid(CreatePaymentRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;



            return isValid;
        }
    }
}