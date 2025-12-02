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
@Constraint(validatedBy = ContractDeliveryActivationTypeValidator.ContractDeliveryActivationTypeValidatorImpl.class)
public @interface ContractDeliveryActivationTypeValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
    class ContractDeliveryActivationTypeValidatorImpl implements ConstraintValidator<ContractDeliveryActivationTypeValidator, BaseTermsRequest> {

        @Override
        public boolean isValid(BaseTermsRequest request, ConstraintValidatorContext context) {
            if (request.getContractDeliveryActivationValue() != null && request.getContractDeliveryActivationType() == null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("contractDeliveryActivationType-Contract Delivery Activation Type is required;")
                        .addConstraintViolation();
                return false;
            }
            if (request.getContractDeliveryActivationValue() == null && request.getContractDeliveryActivationType() != null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("contractDeliveryActivationType-Contract Delivery Activation Type is not required;")
                        .addConstraintViolation();
                return false;
            }
            return true;
        }

    }
}
