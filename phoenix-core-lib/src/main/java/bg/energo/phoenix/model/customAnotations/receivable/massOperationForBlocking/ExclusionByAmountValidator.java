package bg.energo.phoenix.model.customAnotations.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.request.receivable.massOperationForBlocking.ExclusionByAmountRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.util.Objects;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ExclusionByAmountValidator.ExclusionByAmountValidatorImpl.class)
public @interface ExclusionByAmountValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ExclusionByAmountValidatorImpl implements ConstraintValidator<ExclusionByAmountValidator, ExclusionByAmountRequest> {
        @Override
        public boolean isValid(ExclusionByAmountRequest request, ConstraintValidatorContext context) {
            boolean valid = true;

            StringBuilder violations = new StringBuilder();
            context.disableDefaultConstraintViolation();

            BigDecimal greaterThan = request.getGreaterThan();
            BigDecimal lessThan = request.getLessThan();
            Long currency = request.getCurrency();

            if (Objects.isNull(lessThan) && Objects.isNull(greaterThan) && Objects.nonNull(currency)) {
                append(violations, "exclusionByAmount.currency-should be null when neither Less than nor Greater than is defined;");
            }

            if (Objects.isNull(lessThan) && Objects.isNull(greaterThan)) {
                append(violations, "exclusionByAmount.greaterThan-neither Less than nor Greater than must be defined;");
                append(violations, "exclusionByAmount.lessThan-neither Less than nor Greater than must be defined;");
            }

            if (Objects.nonNull(lessThan) || Objects.nonNull(greaterThan)) {
                if (Objects.isNull(currency)) {
                    append(violations, "exclusionByAmount.currency-should not be null when Less than or Greater than is defined;");
                }
            }

            if (Objects.nonNull(lessThan) && Objects.nonNull(greaterThan)) {
                if (greaterThan.compareTo(lessThan) < 0) {
                    append(violations, "exclusionByAmount.lessThan-Less than must be <= Greater than;");
                }
            }

            if (!violations.isEmpty()) {
                valid = false;
                context.buildConstraintViolationWithTemplate(violations.toString()).addConstraintViolation();
            }

            return valid;
        }


        private void append(StringBuilder violations, String message) {
            violations.append(message);
        }

    }
}
