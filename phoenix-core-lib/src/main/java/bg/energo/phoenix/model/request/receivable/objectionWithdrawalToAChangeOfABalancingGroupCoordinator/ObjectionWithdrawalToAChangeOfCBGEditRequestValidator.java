package bg.energo.phoenix.model.request.receivable.objectionWithdrawalToAChangeOfABalancingGroupCoordinator;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = ObjectionWithdrawalToAChangeOfCBGEditRequestValidator.ObjectionWithdrawalToAChangeOfCBGEditRequestValidatorImpl.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ObjectionWithdrawalToAChangeOfCBGEditRequestValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ObjectionWithdrawalToAChangeOfCBGEditRequestValidatorImpl
            implements ConstraintValidator<ObjectionWithdrawalToAChangeOfCBGEditRequestValidator, ObjectionWithdrawalToAChangeOfCBGEditRequest> {

        @Override
        public boolean isValid(ObjectionWithdrawalToAChangeOfCBGEditRequest request, ConstraintValidatorContext context) {
            StringBuilder errors = new StringBuilder();

            if (request.getProcessResultChangeRequests().isEmpty()) {
                errors.append("processResultChangeRequests-processResultChangeRequests must not be empty.");
            }

            if (!errors.isEmpty()) {
                context.buildConstraintViolationWithTemplate(errors.toString()).addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}