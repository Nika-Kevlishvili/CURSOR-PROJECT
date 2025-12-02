package bg.energo.phoenix.model.customAnotations.pod.meter;

import bg.energo.phoenix.model.request.pod.meter.MeterRequest;
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

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidMeterInstallmentRange.MeterInstallmentRangeValidator.class})
public @interface ValidMeterInstallmentRange {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class MeterInstallmentRangeValidator implements ConstraintValidator<ValidMeterInstallmentRange, MeterRequest> {

        private static final LocalDate MIN_DATE = LocalDate.of(1990, Month.JANUARY, 1);
        private static final LocalDate MAX_DATE = LocalDate.of(2090, Month.DECEMBER, 31);

        @Override
        public boolean isValid(MeterRequest request, ConstraintValidatorContext context) {
            LocalDate installmentDate = request.getInstallmentDate();
            LocalDate removeDate = request.getRemoveDate();

            if (installmentDate.isBefore(MIN_DATE) || installmentDate.isAfter(MAX_DATE)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("installmentDate-[Installment Date] must be between %s and %s;"
                        .formatted(MIN_DATE.toString(), MAX_DATE.toString())).addConstraintViolation();
                return false;
            }

            if (removeDate == null) {
                return true;
            }

            if (removeDate.isBefore(MIN_DATE) || removeDate.isAfter(MAX_DATE)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("removeDate-[Remove Date] must be between %s and %s;"
                        .formatted(MIN_DATE.toString(), MAX_DATE.toString())).addConstraintViolation();
                return false;
            }

            if (removeDate.isBefore(installmentDate)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("removeDate-[Remove Date] must be after (or equal) [installmentDate];").addConstraintViolation();
                return false;
            }

            return true;
        }

    }

}
