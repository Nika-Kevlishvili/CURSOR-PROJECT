package bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment;

import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Day;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.Set;


@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {AllWeekDaysSelectedValidator.AllDaysSelectedValidatorImpl.class})
public @interface AllWeekDaysSelectedValidator {
    String message() default "days-When You have selected ALL_DAYS other days must not be selected;";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class AllDaysSelectedValidatorImpl implements ConstraintValidator<AllWeekDaysSelectedValidator, Set<Day>>{

        /**
         * Validate That if Days {@link Day} set contains Day - "ALL_DAYS", other Days are not provided
         *
         * @param days object to validate
         * @param context context in which the constraint is evaluated
         *
         * @return true if constraints are satisfied, else false
         */
        @Override
        public boolean isValid(Set<Day> days, ConstraintValidatorContext context) {
            if(days != null)
                return !days.contains(Day.ALL_DAYS) || days.size() == 1;
            else return true;
        }
    }
}
