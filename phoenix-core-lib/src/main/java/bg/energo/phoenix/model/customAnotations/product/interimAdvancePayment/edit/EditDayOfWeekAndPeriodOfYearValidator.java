package bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment.edit;

import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.edit.EditDayOfWeekPeriodOfYear;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {EditDayOfWeekAndPeriodOfYearValidator.EditDayOfWeekAndPeriodOfYearValidatorImpl.class})
public @interface EditDayOfWeekAndPeriodOfYearValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class EditDayOfWeekAndPeriodOfYearValidatorImpl implements ConstraintValidator<EditDayOfWeekAndPeriodOfYearValidator, EditDayOfWeekPeriodOfYear> {

        /**
         * Validate that if "yearRound" is true "periodsOfYear" is not provided and vice versa
         *
         * @param request object to validate
         * @param context context in which the constraint is evaluated
         *
         * @return true if constraints are satisfied, else false
         */
        @Override
        public boolean isValid(EditDayOfWeekPeriodOfYear request, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            if(request.getYearRound() != null && request.getYearRound() && request.getPeriodsOfYear() != null){
                context.buildConstraintViolationWithTemplate("dayOfWeekAndPeriodOfYear.periodsOfYear-Date ranges must not be provided while yearRound is selected;")
                        .addConstraintViolation();
                return false;
            }
            if(request.getYearRound() != null && !request.getYearRound() && request.getPeriodsOfYear() == null){
                context.buildConstraintViolationWithTemplate("dayOfWeekAndPeriodOfYear.periodsOfYear-Date ranges is required while yearRound is not selected;")
                        .addConstraintViolation();
                return false;
            }
            return true;
        }
    }

}
