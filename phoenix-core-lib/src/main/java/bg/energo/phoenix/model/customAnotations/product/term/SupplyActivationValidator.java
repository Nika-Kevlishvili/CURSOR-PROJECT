package bg.energo.phoenix.model.customAnotations.product.term;

import bg.energo.phoenix.model.enums.product.term.terms.SupplyActivation;
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

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = SupplyActivationValidator.SupplyActivationValidatorImpl.class)
public @interface SupplyActivationValidator {


    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class SupplyActivationValidatorImpl implements ConstraintValidator<SupplyActivationValidator, BaseTermsRequest> {

        @Override
        public boolean isValid(BaseTermsRequest request, ConstraintValidatorContext context) {
            if(request.getSupplyActivations() != null){
                context.disableDefaultConstraintViolation();
//                if(request.getSupplyActivations().contains(SupplyActivation.EXACT_DATE)
//                   && request.getSupplyActivationExactDateStartDay() == null){
//                    context.buildConstraintViolationWithTemplate("supplyActivationExactDateStartDay-Start Day Of Supply Activation is required;")
//                            .addConstraintViolation();
//                    return false;
//                }
                if(!request.getSupplyActivations().contains(SupplyActivation.EXACT_DATE)
                   && request.getSupplyActivationExactDateStartDay() != null){
                    context.buildConstraintViolationWithTemplate("supplyActivationExactDateStartDay-Start Day Of Supply Activation must not be provided;")
                            .addConstraintViolation();
                    return false;
                }
            }
            return true;
        }
    }
}
