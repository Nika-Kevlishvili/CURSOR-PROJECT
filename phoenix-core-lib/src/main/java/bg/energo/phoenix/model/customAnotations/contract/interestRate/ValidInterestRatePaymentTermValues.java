package bg.energo.phoenix.model.customAnotations.contract.interestRate;

import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRatePaymentTermsCalendarType;
import bg.energo.phoenix.model.request.contract.interestRate.InterestRatePaymentTermBaseRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static bg.energo.phoenix.model.enums.contract.InterestRate.InterestRatePaymentTermsCalendarType.WORKING_DAYS;
import static bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateTermsExclude.HOLIDAYS;
import static bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateTermsExclude.WEEKENDS;
import static java.lang.annotation.ElementType.TYPE;

@Target( { TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidInterestRatePaymentTermValues.InterestRatePaymentTermValuesValidator.class})
public @interface ValidInterestRatePaymentTermValues {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class InterestRatePaymentTermValuesValidator implements ConstraintValidator<ValidInterestRatePaymentTermValues, InterestRatePaymentTermBaseRequest> {

        private static final Integer DATE_MIN_VALUE = 0;
        private static final Integer CERTAIN_DATE_MIN_VALUE = 1;
        private static final Integer CERTAIN_DATE_MAX_VALUE = 31;
        private static final Integer WORKING_AND_CALENDAR_DAYS_MAX_VALUE = 9999;

        @Override
        public boolean isValid(InterestRatePaymentTermBaseRequest request, ConstraintValidatorContext context) {
            StringBuilder errorMessage = new StringBuilder();
            Boolean isValid = true;

            InterestRatePaymentTermsCalendarType calendarType = request.getType();
            if (calendarType == null) {
                errorMessage.append("type-Type type must not be null;");
                isValid = false;
                context.buildConstraintViolationWithTemplate(errorMessage.toString()).addConstraintViolation();
                return isValid;
            }

            Integer value = request.getValue();

            if (value == null) {
                errorMessage.append("value-Value must not be null;");
                isValid = false;
                context.buildConstraintViolationWithTemplate(errorMessage.toString()).addConstraintViolation();
                return isValid;
            }

            if (request.getValueTo() != null) {
                errorMessage.append("valueTo-ValueTo should be disabled and empty;");
                isValid = false;
            }

            if (request.getValueFrom() != null) {
                errorMessage.append("valueFrom-ValueFrom should be disabled and empty;");
                isValid = false;
            }

            switch (calendarType) {
                case WORKING_DAYS:
                case CALENDAR_DAYS:
                    if (value < DATE_MIN_VALUE || value > WORKING_AND_CALENDAR_DAYS_MAX_VALUE) {
                        errorMessage.append(String.format("value-Value should be between %s-%s when calendar type is %s;",
                                DATE_MIN_VALUE, WORKING_AND_CALENDAR_DAYS_MAX_VALUE, calendarType.name()));
                        isValid = false;
                    }

                    if (calendarType.equals(WORKING_DAYS) &&
                            (request.getExcludes().contains(WEEKENDS) ||
                                    request.getExcludes().contains(HOLIDAYS))) {
                        errorMessage.append("excludes-Cannot exclude Weekends or Holidays when Calendar Type is set to Working Days.");
                        isValid = false;
                    }
                    break;

                case CERTAIN_DAYS:
                    if (value < CERTAIN_DATE_MIN_VALUE || value > CERTAIN_DATE_MAX_VALUE) {
                        errorMessage.append(String.format("value-Value should be between %s-%s when calendar type is %s;",
                                CERTAIN_DATE_MIN_VALUE, CERTAIN_DATE_MAX_VALUE, calendarType.name()));
                        isValid = false;
                    }
                    break;
            }

            if ((request.getExcludes() == null || request.getExcludes().isEmpty()) && request.getDueDateChange() != null) {
                errorMessage.append("dueDateChange-DueDateChange option cannot be choose when Weekends or Holidays are not selected.;");
                isValid = false;
            }

            context.buildConstraintViolationWithTemplate(errorMessage.toString()).addConstraintViolation();
            return isValid;
        }
    }
}
