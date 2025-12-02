package bg.energo.phoenix.model.customAnotations.product.terms;

import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.request.product.term.terms.BaseTermsRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Constraint(validatedBy = {ValidateStartsOfContractInitialTerms.ValidateStartsOfContractInitialTermsImpl.class})
public @interface ValidateStartsOfContractInitialTerms {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ValidateStartsOfContractInitialTermsImpl implements ConstraintValidator<ValidateStartsOfContractInitialTerms, BaseTermsRequest> {

        @Override
        public boolean isValid(BaseTermsRequest request, ConstraintValidatorContext context) {
            if(request.getStartsOfContractInitialTerms() != null){
                context.disableDefaultConstraintViolation();
                if(request.getStartsOfContractInitialTerms().contains(StartOfContractInitialTerm.FIRST_DAY_MONTH_SIGNING)
                   && request.getFirstDayOfTheMonthOfInitialContractTerm() == null){
                    context.buildConstraintViolationWithTemplate("firstDayOfTheMonthOfInitialContractTerm-Start Day Of Initial Contract Term is required;")
                            .addConstraintViolation();
                    return false;
                }
                if(!request.getStartsOfContractInitialTerms().contains(StartOfContractInitialTerm.EXACT_DATE)
                   && request.getStartDayOfInitialContractTerm() != null){
                    context.buildConstraintViolationWithTemplate("startDayOfInitialContractTerm-Start Day Of Initial Contract Term must not be provided;")
                            .addConstraintViolation();
                    return false;
                }
            }
            return true;
        }
    }

}
