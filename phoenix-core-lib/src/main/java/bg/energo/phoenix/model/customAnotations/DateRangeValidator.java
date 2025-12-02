package bg.energo.phoenix.model.customAnotations;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;

import static java.lang.annotation.ElementType.FIELD;

@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {DateRangeValidator.DateRangeValidatorImpl.class})
public @interface DateRangeValidator {
    String fieldPath() default "";

    boolean includedDate() default true;

    /**
     * A date without a time-zone in the ISO-8601 calendar system, such as 2007-12-03.
     */
    String fromDate() default "";

    /**
     * A date without a time-zone in the ISO-8601 calendar system, such as 2007-12-03.
     */
    String toDate() default "";


    String message() default "Invalid date range provided";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class DateRangeValidatorImpl implements ConstraintValidator<DateRangeValidator, LocalDate> {
        private LocalDate fromDate;
        private LocalDate toDate;
        private String fieldPath;

        private boolean includedDate;

        @Override
        public void initialize(DateRangeValidator constraintAnnotation) {
            try {
                String annotatedFromDate = constraintAnnotation.fromDate();
                if (StringUtils.isNotBlank(annotatedFromDate)) {
                    this.fromDate = new SimpleDateFormat("yyyy-MM-dd")
                            .parse(annotatedFromDate)
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                }
            } catch (ParseException e) {
                throw new IllegalArgumentsProvidedException("Invalid pattern provided for from date in date range validator");
            } catch (Exception e) {
                throw new ClientException("Some error happened in date range validator", ErrorCode.APPLICATION_ERROR);
            }

            try {
                String annotatedToDate = constraintAnnotation.toDate();
                if (StringUtils.isNotBlank(annotatedToDate)) {
                    this.toDate = new SimpleDateFormat("yyyy-MM-dd")
                            .parse(annotatedToDate)
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                }
            } catch (ParseException e) {
                throw new IllegalArgumentsProvidedException("Invalid pattern provided for to date in date range validator");
            } catch (Exception e) {
                throw new ClientException("Some error happened in date range validator", ErrorCode.APPLICATION_ERROR);
            }

            this.fieldPath = constraintAnnotation.fieldPath();
            this.includedDate = constraintAnnotation.includedDate();

            ConstraintValidator.super.initialize(constraintAnnotation);
        }

        @Override
        public boolean isValid(LocalDate validatedValue, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();

            boolean isValid = true;
            StringBuilder violations = new StringBuilder();

            if (validatedValue == null) {
                return true;
            }

            if (fromDate != null && toDate != null) {
                if (fromDate.isAfter(toDate)) {
                    context.buildConstraintViolationWithTemplate("Invalid argument provided in annotation, fromDate must be before toDate;").addConstraintViolation();
                    return false;
                }
            }

            if (includedDate) {
                if (fromDate != null) {
                    if (validatedValue.isBefore(fromDate)) {
                        violations.append("%s-Invalid date provided, must be after or equal to [%s];".formatted(fieldPath, fromDate));
                        isValid = false;
                    }
                }

                if (toDate != null) {
                    if (validatedValue.isAfter(toDate)) {
                        violations.append("%s-Invalid date provided, must be before or equal to [%s];".formatted(fieldPath, toDate));
                        isValid = false;
                    }
                }
            } else {
                if (fromDate != null) {
                    if (validatedValue.isBefore(fromDate)) {
                        violations.append("%s-Invalid date provided, must be after [%s];".formatted(fieldPath, fromDate));
                        isValid = false;
                    }
                }

                if (toDate != null) {
                    if (validatedValue.isAfter(toDate)) {
                        violations.append("%s-Invalid date provided, must be before [%s];".formatted(fieldPath, toDate));
                        isValid = false;
                    }
                }
            }

            if (!isValid) {
                context.buildConstraintViolationWithTemplate(violations.toString()).addConstraintViolation();
            }

            return isValid;
        }
    }
}
