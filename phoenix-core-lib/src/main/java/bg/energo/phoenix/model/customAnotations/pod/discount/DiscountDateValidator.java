package bg.energo.phoenix.model.customAnotations.pod.discount;

import bg.energo.phoenix.model.request.pod.discount.DiscountRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Constraint(validatedBy = {DiscountDateValidator.DiscountDateValidatorImpl.class})
public @interface DiscountDateValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class DiscountDateValidatorImpl implements ConstraintValidator<DiscountDateValidator, DiscountRequest> {

        @Override
        public boolean isValid(DiscountRequest request, ConstraintValidatorContext context) {
            LocalDate dateTo = request.getDateTo();
            LocalDate dateFrom = request.getDateFrom();

            if (dateFrom == null || dateTo == null) {
                return false;
            }

            // Minimum time period for a discount is one day
            LocalDate minimumTimePeriodForDiscount = dateFrom.plusDays(1);

            if (dateTo.isBefore(dateFrom)) {
                context.buildConstraintViolationWithTemplate("dateTo-Date to must be after (or equal) Date From;").addConstraintViolation();
                return false;
            }

            return true;
        }
    }
}
