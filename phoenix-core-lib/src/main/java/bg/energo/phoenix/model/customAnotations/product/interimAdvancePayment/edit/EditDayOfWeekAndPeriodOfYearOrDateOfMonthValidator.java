package bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment.edit;

import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.PeriodType;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditDayOfWeekAndPeriodOfYearAndDateOfMonthRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {EditDayOfWeekAndPeriodOfYearOrDateOfMonthValidator.EditDayOfWeekAndPeriodOfYearOrDateOfMonthValidatorImpl.class})
public @interface EditDayOfWeekAndPeriodOfYearOrDateOfMonthValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class EditDayOfWeekAndPeriodOfYearOrDateOfMonthValidatorImpl implements ConstraintValidator<EditDayOfWeekAndPeriodOfYearOrDateOfMonthValidator, EditDayOfWeekAndPeriodOfYearAndDateOfMonthRequest> {

        /**
         * Validate That If Period Type {@link PeriodType} is "DAY_OF_WEEK_AND_PERIOD_OF_YEAR" "dayOfWeekAndPeriodOfYearAndDateOfMonth" is provided and "dateOfMonths" is not
         * Else "dayOfWeekAndPeriodOfYearAndDateOfMonth" is not provided but "dateOfMonths" is
         *
         * @param period object to validate
         * @param context context in which the constraint is evaluated
         *
         * @return true if constraints are satisfied, else false
         */
        @Override
        public boolean isValid(EditDayOfWeekAndPeriodOfYearAndDateOfMonthRequest period, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            boolean result = true;
            if(period.getPeriodType() != null) {
                if (period.getPeriodType().equals(PeriodType.DAY_OF_WEEK_AND_PERIOD_OF_YEAR)) {
                    if (period.getDayOfWeekAndPeriodOfYear() == null) {
                        context.buildConstraintViolationWithTemplate("dayOfWeekAndPeriodOfYearAndDateOfMonth-is required;")
                                .addConstraintViolation();
                        result = false;
                    }
                    if (period.getDateOfMonths() != null) {
                        context.buildConstraintViolationWithTemplate("dateOfMonths-must not be provided;")
                                .addConstraintViolation();
                        result = false;
                    }
                } else {
                    if (period.getDayOfWeekAndPeriodOfYear() != null) {
                        context.buildConstraintViolationWithTemplate("dayOfWeekAndPeriodOfYearAndDateOfMonth-must not be provided;")
                                .addConstraintViolation();
                        result = false;
                    }
                    if (period.getDateOfMonths() == null) {
                        context.buildConstraintViolationWithTemplate("dateOfMonths-is required;")
                                .addConstraintViolation();
                        result = false;
                    }
                }
            }

            return result;
        }
    }


}
