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
import java.time.LocalDateTime;
import java.time.ZoneId;

import static java.lang.annotation.ElementType.FIELD;

@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {DateTimeRangeValidator.DateTimeRangeValidatorImpl.class})
public @interface DateTimeRangeValidator {
    String fieldPath() default "";

    boolean includedDate() default true;

    String fromDateTime() default "";

    String toDateTime() default "";

    String message() default "Invalid date time range provided";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class DateTimeRangeValidatorImpl implements ConstraintValidator<DateTimeRangeValidator, LocalDateTime> {
        private LocalDateTime fromDateTime;
        private LocalDateTime toDateTime;
        private String fieldPath;

        private boolean includedDate;

        @Override
        public void initialize(DateTimeRangeValidator constraintAnnotation) {
            try {
                String annotatedFromDate = constraintAnnotation.fromDateTime();
                if (StringUtils.isNotBlank(annotatedFromDate)) {
                    this.fromDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm")
                            .parse(annotatedFromDate)
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                }
            } catch (ParseException e) {
                throw new IllegalArgumentsProvidedException("Invalid pattern provided for from date time in date time range validator");
            } catch (Exception e) {
                throw new ClientException("Some error happened in date range validator", ErrorCode.APPLICATION_ERROR);
            }

            try {
                String annotatedToDate = constraintAnnotation.toDateTime();
                if (StringUtils.isNotBlank(annotatedToDate)) {
                    this.toDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm")
                            .parse(annotatedToDate)
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                }
            } catch (ParseException e) {
                throw new IllegalArgumentsProvidedException("Invalid pattern provided for to date time in date time range validator");
            } catch (Exception e) {
                throw new ClientException("Some error happened in date range validator", ErrorCode.APPLICATION_ERROR);
            }

            this.fieldPath = constraintAnnotation.fieldPath();
            this.includedDate = constraintAnnotation.includedDate();

            ConstraintValidator.super.initialize(constraintAnnotation);
        }

        @Override
        public boolean isValid(LocalDateTime validatedValue, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();

            boolean isValid = true;
            StringBuilder violations = new StringBuilder();

            if (validatedValue == null) {
                return true;
            }

            if (fromDateTime != null && toDateTime != null) {
                if (fromDateTime.isAfter(toDateTime)) {
                    context.buildConstraintViolationWithTemplate("Invalid argument provided in annotation, fromDateTime must be before toDateTime;").addConstraintViolation();
                    return false;
                }
            }

            if (includedDate) {
                if (fromDateTime != null) {
                    if (validatedValue.isBefore(fromDateTime)) {
                        violations.append("%s-Invalid dateTime provided, must be after or equal to [%s];".formatted(fieldPath, fromDateTime));
                        isValid = false;
                    }
                }

                if (toDateTime != null) {
                    if (validatedValue.isAfter(toDateTime)) {
                        violations.append("%s-Invalid dateTime provided, must be before or equal to [%s];".formatted(fieldPath, toDateTime));
                        isValid = false;
                    }
                }
            } else {
                if (fromDateTime != null) {
                    if (validatedValue.isBefore(fromDateTime)) {
                        violations.append("%s-Invalid dateTime provided, must be after [%s];".formatted(fieldPath, fromDateTime));
                        isValid = false;
                    }
                }

                if (toDateTime != null) {
                    if (validatedValue.isAfter(toDateTime)) {
                        violations.append("%s-Invalid dateTime provided, must be before [%s];".formatted(fieldPath, toDateTime));
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
