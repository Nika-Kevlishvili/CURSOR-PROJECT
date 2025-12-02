package bg.energo.phoenix.model.customAnotations.receivable.payment;

import bg.energo.phoenix.model.request.receivable.payment.PaymentCancelRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
@Constraint(validatedBy = PaymentCancelRequestValidator.PaymentCancelRequestValidatorImpl.class)
public @interface PaymentCancelRequestValidator {
    String value() default "";

    String message() default "Invalid payment cancellation request";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PaymentCancelRequestValidatorImpl implements ConstraintValidator<PaymentCancelRequestValidator, PaymentCancelRequest> {
        @Override
        public boolean isValid(PaymentCancelRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessages = new StringBuilder();

            validateBlockingForOffsetting(request, validationMessages);

            if (!validationMessages.isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessages.toString())
                        .addConstraintViolation();
                isValid = false;
            }
            return isValid;
        }

        private void validateBlockingForOffsetting(PaymentCancelRequest request, StringBuilder validationMessages) {
            if (Boolean.TRUE.equals(request.getBlockedForOffsetting())) {
                if (request.getBlockedForOffsettingFromDate() == null) {
                    validationMessages.append("Block from date is required when blocking for offsetting is selected;");
                }

                if (request.getReasonId() == null) {
                    validationMessages.append("Reason is required when blocking for offsetting is selected;");
                }
            }
        }
    }
}
