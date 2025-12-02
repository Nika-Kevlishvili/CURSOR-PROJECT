package bg.energo.phoenix.model.request.receivable.balancingGroupCoordinatorObjection;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = BalancingGroupCoordinatorObjectionEditRequestValidator.BalancingGroupCoordinatorObjectionEditRequestValidatorImpl.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface BalancingGroupCoordinatorObjectionEditRequestValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class BalancingGroupCoordinatorObjectionEditRequestValidatorImpl
            implements ConstraintValidator<BalancingGroupCoordinatorObjectionEditRequestValidator, BalancingGroupCoordinatorObjectionEditRequest> {

        @Override
        public boolean isValid(BalancingGroupCoordinatorObjectionEditRequest request, ConstraintValidatorContext context) {
            StringBuilder errors = new StringBuilder();

            if (request.getFileId() == null) {
                if (request.getProcessEditRequest() == null || request.getProcessEditRequest().isEmpty()) {
                    errors.append("processEditRequest-processEditRequest must not be empty.");
                }
            }
            if (!errors.isEmpty()) {
                context.buildConstraintViolationWithTemplate(errors.toString()).addConstraintViolation();
                return false;
            }
            return true;
        }
    }
}