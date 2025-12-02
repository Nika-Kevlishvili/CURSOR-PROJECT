package bg.energo.phoenix.model.customAnotations.customer.communicationData;

import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.request.customer.communicationData.contactPerson.BaseContactPersonRequest;
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
@Constraint(validatedBy = ValidContactPersonRequest.ContactPersonRequestValidator.class)
public @interface ValidContactPersonRequest {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ContactPersonRequestValidator implements ConstraintValidator<ValidContactPersonRequest, BaseContactPersonRequest> {

        @Override
        public boolean isValid(BaseContactPersonRequest contactPersonRequest, ConstraintValidatorContext context) {
            boolean valid = true;
            StringBuilder stringBuilder = new StringBuilder("Manager requests: ");

            if (contactPersonRequest.getStatus() != null && contactPersonRequest.getStatus().equals(Status.DELETED)) {
                stringBuilder.append("communicationData.contactPersons.status-Status should not be DELETED in the request; ");
                valid = false;
            }

            if (contactPersonRequest.getPositionHeldTo() != null && contactPersonRequest.getPositionHeldFrom() == null) {
                stringBuilder.append("communicationData.contactPersons.positionHeldFrom-Position held from is mandatory when position held to is not empty; ");
                valid = false;
            }

            if (contactPersonRequest.getPositionHeldTo() != null
                    && contactPersonRequest.getPositionHeldFrom() != null
                    && contactPersonRequest.getPositionHeldTo().isBefore(contactPersonRequest.getPositionHeldFrom())) {
                stringBuilder.append("communicationData.contactPersons.PositionHeldTo-Position-held-to date should not be before the position-held-from date; ");
                valid = false;
            }

            context.buildConstraintViolationWithTemplate(stringBuilder.toString()).addConstraintViolation();
            return valid;
        }

    }
}
