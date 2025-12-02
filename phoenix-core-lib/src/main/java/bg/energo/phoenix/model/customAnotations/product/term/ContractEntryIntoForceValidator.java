package bg.energo.phoenix.model.customAnotations.product.term;

import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
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
@Constraint(validatedBy = ContractEntryIntoForceValidator.ContractEntryIntoForceValidatorImpl.class)
public @interface ContractEntryIntoForceValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ContractEntryIntoForceValidatorImpl implements ConstraintValidator<ContractEntryIntoForceValidator, BaseTermsRequest> {

        @Override
        public boolean isValid(BaseTermsRequest request, ConstraintValidatorContext context) {
            if(request.getContractEntryIntoForces() != null){
                context.disableDefaultConstraintViolation();
//                if(request.getContractEntryIntoForces().contains(ContractEntryIntoForce.EXACT_DAY_OF_MONTH)
//                   && request.getContractEntryIntoForceFromExactDayOfMonthStartDay() == null){
//                    context.buildConstraintViolationWithTemplate("contractEntryIntoForceFromExactDayOfMonthStartDay-Contract Entry Into Force Start Day is required;")
//                            .addConstraintViolation();
//                    return false;
//                }
                if(!request.getContractEntryIntoForces().contains(ContractEntryIntoForce.EXACT_DAY)
                   && request.getContractEntryIntoForceFromExactDayOfMonthStartDay() != null){
                    context.buildConstraintViolationWithTemplate("contractEntryIntoForceFromExactDayOfMonthStartDay-Contract Entry Into Force Start Day must not be provided;")
                            .addConstraintViolation();
                    return false;
                }
            }
            return true;
        }
    }

}
