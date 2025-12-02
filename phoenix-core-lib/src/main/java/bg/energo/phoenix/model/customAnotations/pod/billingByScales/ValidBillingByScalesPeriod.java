package bg.energo.phoenix.model.customAnotations.pod.billingByScales;

import bg.energo.phoenix.model.request.pod.billingByScales.BillingByScalesCreateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidBillingByScalesPeriod.BillingByScalesPeriodValidator.class})
public @interface ValidBillingByScalesPeriod {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class BillingByScalesPeriodValidator implements ConstraintValidator<ValidBillingByScalesPeriod, BillingByScalesCreateRequest> {

        private static final LocalDate MIN_DATE = LocalDate.of(1990, Month.JANUARY, 1);
        private static final LocalDate MAX_DATE = LocalDate.of(2090, Month.DECEMBER, 31);

        @Override
        public boolean isValid(BillingByScalesCreateRequest request, ConstraintValidatorContext context) {
            LocalDate periodFrom = request.getDateFrom();
            LocalDate periodTo = request.getDateTo();

            if (periodFrom != null && periodTo != null) {
                if (periodFrom.isBefore(MIN_DATE) || periodFrom.isAfter(MAX_DATE)) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("periodFrom-[PeriodFrom] must be between %s and %s;"
                            .formatted(MIN_DATE.toString(), MAX_DATE.toString())).addConstraintViolation();
                    return false;
                }

                if (periodTo.isBefore(MIN_DATE) || periodTo.isAfter(MAX_DATE)) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("periodTo-[PeriodTo] must be between %s and %s;"
                            .formatted(MIN_DATE.toString(), MAX_DATE.toString())).addConstraintViolation();
                    return false;
                }

                if (periodTo.isBefore(periodFrom)) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("periodTo-[PeriodTo] must be before (or equal) [periodFrom];").addConstraintViolation();
                    return false;
                }
                long years = ChronoUnit.YEARS.between(periodFrom, periodTo);
                if (years > 1) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("periodFrom-[PeriodFrom] Period should be limited to one-year time interval;").addConstraintViolation();
                    context.buildConstraintViolationWithTemplate("periodTo-[PeriodTo] Period should be limited to one-year time interval;").addConstraintViolation();
                    return false;
                }

                return true;
            } else {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("periodFrom-[PeriodFrom] Period should be limited to one-year time interval;").addConstraintViolation();
                context.buildConstraintViolationWithTemplate("periodTo-[PeriodTo] Period should be limited to one-year time interval;").addConstraintViolation();
                return false;
            }
        }
    }
}
