package bg.energo.phoenix.model.customAnotations.product.interimAdvancePayment;

import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.DateOfIssueType;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.CreateInterimAdvancePaymentRequest;
import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.DayOfWeekAndPeriodOfYearAndDateOfMonthRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ValidationsForPeriodicalDateOfIssueType.ValidationsForPeriodicalDateOfIssueTypeImpl.class})
public @interface ValidationsForPeriodicalDateOfIssueType {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ValidationsForPeriodicalDateOfIssueTypeImpl implements ConstraintValidator<ValidationsForPeriodicalDateOfIssueType, CreateInterimAdvancePaymentRequest> {

        /**
         * Check if Date Of Issue Type is Periodical and periods request is provided
         * Or Type is not periodical and request is not provided {@link DateOfIssueType}, {@link DayOfWeekAndPeriodOfYearAndDateOfMonthRequest}
         *
         * @param request object to validate
         * @param context context in which the constraint is evaluated
         *
         * @return false if constraints are violated, else true
         */
        @Override
        public boolean isValid(CreateInterimAdvancePaymentRequest request, ConstraintValidatorContext context) {
            boolean result = true;
            if(request.getDateOfIssueType() != null){
                if(request.getDateOfIssueType().equals(DateOfIssueType.PERIODICAL)){
                    if(request.getDayOfWeekAndPeriodOfYearAndDateOfMonth() == null){
                        context.buildConstraintViolationWithTemplate("dayOfWeekAndPeriodOfYearAndDateOfMonth-is required;")
                                .addConstraintViolation();
                        result = false;
                    }
                }else {
                    if(request.getDayOfWeekAndPeriodOfYearAndDateOfMonth() != null){
                        context.buildConstraintViolationWithTemplate("dayOfWeekAndPeriodOfYearAndDateOfMonth-Must not be provided;")
                                .addConstraintViolation();
                        result = false;
                    }
                }

            }
            return result;
        }
    }
}
