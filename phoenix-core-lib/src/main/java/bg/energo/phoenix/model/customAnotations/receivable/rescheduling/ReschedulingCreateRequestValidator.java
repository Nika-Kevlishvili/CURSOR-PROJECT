package bg.energo.phoenix.model.customAnotations.receivable.rescheduling;

import bg.energo.phoenix.model.request.receivable.rescheduling.ReschedulingRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.math.BigDecimal;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
@Constraint(validatedBy = bg.energo.phoenix.model.customAnotations.receivable.rescheduling.ReschedulingCreateRequestValidator.ReschedulingCreateRequestValidatorImpl.class)
public @interface ReschedulingCreateRequestValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


    class ReschedulingCreateRequestValidatorImpl implements ConstraintValidator<bg.energo.phoenix.model.customAnotations.receivable.rescheduling.ReschedulingCreateRequestValidator, ReschedulingRequest> {
        @Override
        public boolean isValid(ReschedulingRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessages = new StringBuilder();

            validateInstallments(request, validationMessages);

            if (!validationMessages.isEmpty()) {
                context.buildConstraintViolationWithTemplate(validationMessages.toString()).addConstraintViolation();
                isValid = false;
            }
            return isValid;
        }

        private void validateInstallments(ReschedulingRequest request, StringBuilder validationMessages) {
            BigDecimal numberOfInstallment = request.getNumberOfInstallment();
            BigDecimal amountOfTheInstallment = request.getAmountOfTheInstallment();

            if (numberOfInstallment == null && amountOfTheInstallment == null) {
                validationMessages.append("Either number of installments or amount of the installment must be provided;");
            }

            if (numberOfInstallment != null && amountOfTheInstallment != null) {
                validationMessages.append("Cannot specify both number of installments and amount of the installment");
            }
        }
    }
}
