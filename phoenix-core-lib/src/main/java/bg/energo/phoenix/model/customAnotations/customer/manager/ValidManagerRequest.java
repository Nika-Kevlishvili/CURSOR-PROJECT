package bg.energo.phoenix.model.customAnotations.customer.manager;

import bg.energo.phoenix.model.enums.customer.Status;
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
@Constraint(validatedBy = ValidManagerRequest.ManagerRequestValidator.class)
public @interface ValidManagerRequest {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ManagerRequestValidator implements ConstraintValidator<ValidManagerRequest, bg.energo.phoenix.model.request.customer.manager.BaseManagerRequest> {

        @Override
        public boolean isValid(bg.energo.phoenix.model.request.customer.manager.BaseManagerRequest managerRequest, ConstraintValidatorContext context) {
            boolean valid = true;
            StringBuilder stringBuilder = new StringBuilder();

            if (managerRequest.getStatus() != null && managerRequest.getStatus().equals(Status.DELETED)) {
                stringBuilder.append("managers.status-Status should not be DELETED in the request; ");
                valid = false;
            }

            if (managerRequest.getPositionHeldTo() != null && managerRequest.getPositionHeldFrom() == null) {
                stringBuilder.append("managers.positionHeldTo-Position held from is mandatory when position held to is not empty; ");
                valid = false;
            }

            if (managerRequest.getPositionHeldTo() != null
                    && managerRequest.getPositionHeldFrom() != null
                    && managerRequest.getPositionHeldTo().isBefore(managerRequest.getPositionHeldFrom())) {
                stringBuilder.append("managers.positionHeldFrom-Position-held-to date should not be before the position-held-from date; ");
                valid = false;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
            return valid;
        }

    }
}
