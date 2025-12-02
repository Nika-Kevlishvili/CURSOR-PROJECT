package bg.energo.phoenix.model.customAnotations.contract.express;

import bg.energo.phoenix.model.request.contract.express.ExpressContractCustomerRequest;
import bg.energo.phoenix.util.customer.CustomerIdentifierValidator;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ExpressCustomerIdentifierValidator.ExpressCustomerIdentifierValidatorImpl.class})
public @interface ExpressCustomerIdentifierValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ExpressCustomerIdentifierValidatorImpl implements ConstraintValidator<ExpressCustomerIdentifierValidator, ExpressContractCustomerRequest> {

        /**
         * Validates customer identifier based on customer Type:
         * if LEGAL_ENTITY length should be between 1-256 digits
         * if PRIVATE length must be 10 (Local customer) or 12 (Foreign customer) characters
         *
         * @param request        CreateCustomerRequest object with customerIdentifier, customerType and foreign fields
         * @param context        context in which the constraint is evaluated
         * @return               true if customerIdentifier is valid, false otherwise
         */
        @Override
        public boolean isValid(ExpressContractCustomerRequest request, ConstraintValidatorContext context) {
            StringBuilder validationMessage = new StringBuilder();

            boolean isValid = CustomerIdentifierValidator.isValidCustomerIdentifier(
                    request.getIdentifier(),
                    request.getCustomerType(),
                    request.isForeign(),
                    validationMessage
            );

            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("customer.identifier-Invalid identifier;").addConstraintViolation();
            }

            return isValid;
        }

    }

}
