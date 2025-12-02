package bg.energo.phoenix.model.customAnotations.product.term;

import bg.energo.phoenix.model.request.product.term.terms.BaseTermsRequest;
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
@Constraint(validatedBy = ContractDeliveryActivationAutoTerminationValidator.ContractDeliveryActivationAutoTerminationValidatorImpl.class)
public @interface ContractDeliveryActivationAutoTerminationValidator {
    String message() default "contractDeliveryActivationAutoTermination-Contract Delivery Activation Auto Termination is not required;";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
    class ContractDeliveryActivationAutoTerminationValidatorImpl implements ConstraintValidator<ContractDeliveryActivationAutoTerminationValidator, BaseTermsRequest> {

        @Override
        public boolean isValid(BaseTermsRequest request, ConstraintValidatorContext context) {
            return request.getContractDeliveryActivationValue() != null
                   || (request.getContractDeliveryActivationAutoTermination() == null
                       || !request.getContractDeliveryActivationAutoTermination());
        }
    }
}
