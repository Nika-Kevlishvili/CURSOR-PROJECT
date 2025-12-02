package bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment;

import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.MonthNumber;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.Set;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {AllMonthDaysSelectedValidator.AllMonthDaysSelectedValidatorImpl.class})
public @interface AllMonthDaysSelectedValidator {

    String message() default "dateOfMonths.monthNumbers-When You have selected ALL_DAYS other days must not be selected;";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class AllMonthDaysSelectedValidatorImpl implements ConstraintValidator<AllMonthDaysSelectedValidator, Set<MonthNumber>> {

        /**
         * Validate That if MonthNumbers {@link MonthNumber} set contains Month Number - "ALL_DAYS", other Month Numbers are not provided
         *
         * @param monthNumbers object to validate
         * @param context context in which the constraint is evaluated
         *
         * @return true if constraints are satisfied, else false
         */
        @Override
        public boolean isValid(Set<MonthNumber> monthNumbers, ConstraintValidatorContext context) {
            if(monthNumbers != null)
                return !monthNumbers.contains(MonthNumber.ALL_DAYS) || monthNumbers.size() == 1;
            else return true;
        }
    }

}
