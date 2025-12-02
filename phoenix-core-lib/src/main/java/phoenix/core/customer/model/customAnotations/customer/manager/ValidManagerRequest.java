package phoenix.core.customer.model.customAnotations.customer.manager;

import phoenix.core.customer.model.enums.customer.Status;
import phoenix.core.customer.model.request.manager.BaseManagerRequest;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
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

    class ManagerRequestValidator implements ConstraintValidator<ValidManagerRequest, BaseManagerRequest> {

        @Override
        public boolean isValid(BaseManagerRequest managerRequest, ConstraintValidatorContext context) {
            boolean valid = true;
            StringBuilder stringBuilder = new StringBuilder("Manager requests: ");

            if (managerRequest.getStatus() != null && managerRequest.getStatus().equals(Status.DELETED)) {
                stringBuilder.append("Status should not be DELETED in the request; ");
                valid = false;
            }

            if (managerRequest.getPositionHeldTo() != null && managerRequest.getPositionHeldFrom() == null) {
                stringBuilder.append("Position held from is mandatory when position held to is not empty; ");
                valid = false;
            }

            if (managerRequest.getPositionHeldTo() != null
                    && managerRequest.getPositionHeldFrom() != null
                    && managerRequest.getPositionHeldTo().isBefore(managerRequest.getPositionHeldFrom())) {
                stringBuilder.append("Position-held-to date should not be before the position-held-from date; ");
                valid = false;
            }

            context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
            return valid;
        }

    }
}
