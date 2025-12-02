package bg.energo.phoenix.model.customAnotations.receivable.deposit;

import bg.energo.phoenix.model.request.receivable.deposit.DepositCreateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = DepositRequestValidator.DepositRequestValidatorImpl.class)
public @interface DepositRequestValidator {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class DepositRequestValidatorImpl implements ConstraintValidator<DepositRequestValidator, DepositCreateRequest> {
        private final LocalDate MIN_DATE = LocalDate.now();
        private final LocalDate MAX_DATE = LocalDate.of(2090, 12, 31);

        @Override
        public boolean isValid(DepositCreateRequest request, ConstraintValidatorContext context) {
            boolean isValid = false;
            StringBuilder validationMessages = new StringBuilder();

            if(request.getPaymentDeadline() == null) {
                validationMessages.append("paymentDeadline-[paymentDeadline] must not be null;");
                context.buildConstraintViolationWithTemplate(validationMessages.toString()).addConstraintViolation();
                return false;
            }

            LocalDate dateToValidate = request.getPaymentDeadline();

            if(dateToValidate.isEqual(MIN_DATE)) {
                isValid = true;
            }
            if(dateToValidate.isEqual(MAX_DATE)){
                isValid = true;
            }
            if(dateToValidate.isAfter(MIN_DATE) && dateToValidate.isBefore(MAX_DATE)) {
                isValid = true;
            }

            if(!isValid) {
                validationMessages.append("paymentDeadline-[paymentDeadline] must be from present to 31.12.2090");
            }

            if (!validationMessages.isEmpty()) {
                context.buildConstraintViolationWithTemplate(validationMessages.toString()).addConstraintViolation();
                isValid = false;
            }

            return isValid;
        }

    }
}
