package bg.energo.phoenix.model.customAnotations.customer.withValidators.CustomerEditValidators;

import bg.energo.phoenix.model.request.customer.EditCustomerRequest;
import bg.energo.phoenix.util.customer.CustomerIdentifierValidator;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidCustomerIdentifierOnEdit.CustomerIdentifierValidatorOnEditImpl.class})
public @interface ValidCustomerIdentifierOnEdit {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class CustomerIdentifierValidatorOnEditImpl implements ConstraintValidator<ValidCustomerIdentifierOnEdit, EditCustomerRequest> {

        /**
         * Validates customer identifier based on customer Type:
         * if LEGAL_ENTITY length should be between 1-256 digits
         * if PRIVATE length must be 10 (Local customer) or 12 (Foreign customer) characters
         *
         * @param request       object to validate
         * @param context       context in which the constraint is evaluated
         * @return              true if customerIdentifier is valid, false otherwise
         */
        @Override
        public boolean isValid(EditCustomerRequest request, ConstraintValidatorContext context) {
            if (request.getCustomerIdentifier().equals(EPBFinalFields.GDPR)){
                return true;
            }

            StringBuilder validationMessage = new StringBuilder();

            boolean isValid = CustomerIdentifierValidator.isValidCustomerIdentifier(
                    request.getCustomerIdentifier(),
                    request.getCustomerType(),
                    request.getForeign(),
                    validationMessage
            );

            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessage.toString()).addConstraintViolation();
            }

            return isValid;
        }

    }
}
