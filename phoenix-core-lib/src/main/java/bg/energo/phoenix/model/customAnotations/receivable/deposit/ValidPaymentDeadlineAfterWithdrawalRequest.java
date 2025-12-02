package bg.energo.phoenix.model.customAnotations.receivable.deposit;

import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.request.receivable.deposit.PaymentDeadlineAfterWithdrawalRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target( { TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidPaymentDeadlineAfterWithdrawalRequest.PaymentDeadlineAfterWithdrawalRequestValidator.class})
public @interface ValidPaymentDeadlineAfterWithdrawalRequest {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


    class PaymentDeadlineAfterWithdrawalRequestValidator implements ConstraintValidator<ValidPaymentDeadlineAfterWithdrawalRequest, PaymentDeadlineAfterWithdrawalRequest> {

        private static final Integer DATE_MIN_VALUE = 0;
        private static final Integer CERTAIN_DATE_MIN_VALUE = 1;
        private static final Integer CERTAIN_DATE_MAX_VALUE = 31;
        private static final Integer WORKING_AND_CALENDAR_DAYS_MAX_VALUE = 9999;

        @Override
        public boolean isValid(PaymentDeadlineAfterWithdrawalRequest request, ConstraintValidatorContext context) {
            StringBuilder errorMessage = new StringBuilder();
            Boolean isValid = true;

            CalendarType type = request.getCalendarType();

            if (type == null) {
                errorMessage.append("calendarType-[calendarType] type must not be null;");
                isValid = false;
                return isValid;
            }
            if (request.getExcludeHolidays() == null) {
                errorMessage.append("excludeHolidays-[excludeHolidays] must not be null;");
                return false;
            }
            if (request.getExcludeWeekends() == null) {
                errorMessage.append("excludeWeekends-[excludeWeekends] must not be null;");
                return false;
            }
            context.buildConstraintViolationWithTemplate(errorMessage.toString()).addConstraintViolation();

            Integer value = request.getValue();

            if (value == null) {
                return isValid;
            }

            switch (type) {
                case WORKING_DAYS:
                case CALENDAR_DAYS:
                    isValid = checkValuesAgainstType(
                            errorMessage,
                            type,
                            value,
                            WORKING_AND_CALENDAR_DAYS_MAX_VALUE,
                            DATE_MIN_VALUE,
                            isValid
                    );

                    if (type.equals(CalendarType.WORKING_DAYS) &&
                            (Boolean.TRUE.equals(request.getExcludeHolidays()) || Boolean.TRUE.equals(request.getExcludeWeekends()))) {
                        errorMessage.append("excludeWeekends/excludeHolidays-Cannot exclude Weekends or Holidays when Calendar Type is set to Working Days.");
                        isValid = false;
                    }
                    break;

                case CERTAIN_DAYS:
                    isValid = checkValuesAgainstType(
                            errorMessage,
                            type,
                            value,
                            CERTAIN_DATE_MAX_VALUE,
                            CERTAIN_DATE_MIN_VALUE,
                            isValid
                    );
                    break;
            }

            if (!request.getExcludeWeekends() && !request.getExcludeHolidays() && request.getDueDateChange() != null) {
                errorMessage.append("dueDateChange-[DueDateChange] option cannot be choose when Weekends or Holidays are not selected.;");
                isValid = false;
            }

            context.buildConstraintViolationWithTemplate(errorMessage.toString()).addConstraintViolation();
            return isValid;
        }

        private Boolean checkValuesAgainstType(StringBuilder errorMessage,
                                               CalendarType type,
                                               Integer value,
                                               Integer maxValue,
                                               Integer minValue,
                                               Boolean isValid) {
            if (value != null && (value < minValue || value > maxValue)) {
                errorMessage.append(String.format("value-[value] should be between %s-%s when type is %s;",
                        minValue, maxValue, type.name()));
                isValid = false;
            }

            return isValid;

        }
    }
}
