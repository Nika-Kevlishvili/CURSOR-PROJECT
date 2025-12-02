package bg.energo.phoenix.model.customAnotations.product.terms;

import bg.energo.phoenix.model.enums.product.term.terms.CalendarType;
import bg.energo.phoenix.model.request.product.term.terms.paymentTerm.BasePaymentTermsRequest;
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
@Constraint(validatedBy = {ValidInvoicePaymentTermValues.InvoicePaymentTermValuesValidator.class})
public @interface ValidInvoicePaymentTermValues {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class InvoicePaymentTermValuesValidator implements ConstraintValidator<ValidInvoicePaymentTermValues, BasePaymentTermsRequest> {

        private static final Integer DATE_MIN_VALUE = 0;
        private static final Integer CERTAIN_DATE_MIN_VALUE = 1;
        private static final Integer CERTAIN_DATE_MAX_VALUE = 31;
        private static final Integer WORKING_AND_CALENDAR_DAYS_MAX_VALUE = 9999;

        @Override
        public boolean isValid(BasePaymentTermsRequest request, ConstraintValidatorContext context) {
            StringBuilder errorMessage = new StringBuilder();
            Boolean isValid = true;

            CalendarType calendarType = request.getCalendarType();
            if (calendarType == null) {
                errorMessage.append("calendarType-Calendar type must not be null;");
                isValid = false;
                return isValid;
            }

            Integer value = request.getValue();
            Integer valueFrom = request.getValueFrom();
            Integer valueTo = request.getValueTo();

            if (value == null && valueFrom == null && valueTo == null) {
                return isValid;
            }

            switch (calendarType) {
                case WORKING_DAYS:
                case CALENDAR_DAYS:
                    isValid = checkValuesAgainstCalendarType(
                            errorMessage,
                            calendarType,
                            value,
                            valueFrom,
                            valueTo,
                            WORKING_AND_CALENDAR_DAYS_MAX_VALUE,
                            DATE_MIN_VALUE,
                            isValid
                    );

                    if (calendarType.equals(CalendarType.WORKING_DAYS) &&
                            (Boolean.TRUE.equals(request.getExcludeHolidays()) || Boolean.TRUE.equals(request.getExcludeWeekends()))) {
                        errorMessage.append("excludeWeekends/excludeHolidays-Cannot exclude Weekends or Holidays when Calendar Type is set to Working Days.");
                        isValid = false;
                    }
                    break;

                case CERTAIN_DAYS:
                    isValid = checkValuesAgainstCalendarType(
                            errorMessage,
                            calendarType,
                            value,
                            valueFrom,
                            valueTo,
                            CERTAIN_DATE_MAX_VALUE,
                            CERTAIN_DATE_MIN_VALUE,
                            isValid
                    );
                    break;
            }

            if (value != null) {
                if (valueFrom != null && value < valueFrom) {
                    errorMessage.append("value-Value should not be less than valueFrom;");
                    isValid = false;
                }

                if (valueTo != null && value > valueTo) {
                    errorMessage.append("value-Value should not be less than valueTo;");
                    isValid = false;
                }
            }

            if (valueFrom != null) {
                if (value != null && valueFrom > value) {
                    errorMessage.append("valueFrom-ValueFrom should not be greater than value;");
                    isValid = false;
                }

                if (valueTo != null && valueFrom >= valueTo) {
                    errorMessage.append("valueFrom-ValueFrom should not be greater or equal to valueTo;");
                    isValid = false;
                }
            }

            if (valueTo != null) {
                if (value != null && value > valueTo) {
                    errorMessage.append("valueTo-ValueTo should not be less than value;");
                    isValid = false;
                }

                if (valueFrom != null && valueFrom >= valueTo) {
                    errorMessage.append("valueTo-ValueTo should not be less or equal to valueFrom;");
                    isValid = false;
                }
            }

            if (!request.getExcludeWeekends() && !request.getExcludeHolidays() && request.getDueDateChange() != null) {
                errorMessage.append("dueDateChange-DueDateChange option cannot be choose when Weekends or Holidays are not selected.;");
                isValid = false;
            }

            context.buildConstraintViolationWithTemplate(errorMessage.toString()).addConstraintViolation();
            return isValid;
        }

        private Boolean checkValuesAgainstCalendarType(StringBuilder errorMessage,
                                                    CalendarType calendarType,
                                                    Integer value,
                                                    Integer valueFrom,
                                                    Integer valueTo,
                                                    Integer maxValue,
                                                    Integer minValue,
                                                    Boolean isValid) {
            if (value != null && (value < minValue || value > maxValue)) {
                errorMessage.append(String.format("value-Value should be between %s-%s when calendar type is %s;",
                        minValue, maxValue, calendarType.name()));
                isValid = false;
            }

            if (valueFrom != null && (valueFrom < minValue || valueFrom > maxValue)) {
                errorMessage.append(String.format("valueFrom-ValueFrom should be between %s-%s when calendar type is %s;",
                        minValue, maxValue, calendarType.name()));
                isValid = false;
            }

            if (valueTo != null && (valueTo < minValue || valueTo > maxValue)) {
                errorMessage.append(String.format("valueTo-ValueTo should be between %s-%s when calendar type is %s;",
                        minValue, maxValue, calendarType.name()));
                isValid = false;
            }

            return isValid;
        }
    }
}
