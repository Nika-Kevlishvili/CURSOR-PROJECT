package bg.energo.phoenix.model.customAnotations.product.terms;

import bg.energo.phoenix.model.enums.product.term.terms.SupplyActivation;
import bg.energo.phoenix.model.request.product.term.terms.BaseTermsRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Constraint(validatedBy = {WaitForOldContractTermToExpireValidator.WaitForOldContractTermToExpireValidatorImpl.class})
public @interface WaitForOldContractTermToExpireValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class WaitForOldContractTermToExpireValidatorImpl implements ConstraintValidator<WaitForOldContractTermToExpireValidator, BaseTermsRequest> {

        @Override
        public boolean isValid(BaseTermsRequest request, ConstraintValidatorContext context) {
            if (request.getSupplyActivations() != null && request.getWaitForOldContractTermToExpires() != null) {
                context.disableDefaultConstraintViolation();
                if (!request.getSupplyActivations().contains(SupplyActivation.FIRST_DAY_OF_MONTH) && !request.getSupplyActivations().contains(SupplyActivation.EXACT_DATE) && !request.getWaitForOldContractTermToExpires().isEmpty()) {
                    context.buildConstraintViolationWithTemplate("waitForOldContractTermToExpires-waitForOldContractTermToExpires must be empty;")
                            .addConstraintViolation();
                    return false;
                }
            } else {
                if (request.getSupplyActivations() == null && request.getWaitForOldContractTermToExpires() != null) {
                    if (!request.getWaitForOldContractTermToExpires().isEmpty()) {
                        context.buildConstraintViolationWithTemplate("waitForOldContractTermToExpires-waitForOldContractTermToExpires must be empty;")
                                .addConstraintViolation();
                        return false;
                    }
                }
            }
            return true;
        }
    }

}
