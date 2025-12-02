package bg.energo.phoenix.model.customAnotations.contract.billing;

import bg.energo.phoenix.model.request.contract.billing.BillingGroupRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {BillingGroupRequestValidator.validBillingGroupRequest.class})
public @interface BillingGroupRequestValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class validBillingGroupRequest implements ConstraintValidator<BillingGroupRequestValidator, BillingGroupRequest> {

        @Override
        public boolean isValid(BillingGroupRequest request, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            StringBuilder errorMessage = new StringBuilder();

            if (request.isDirectDebit()) {
                if (request.getBankId() == null) {
                    errorMessage.append("bankId-Bank is mandatory when direct debit is selected;");
                }

                if (StringUtils.isEmpty(request.getIban())) {
                    errorMessage.append("iban-IBAN is mandatory when direct debit is selected;");
                }
            }

            if (!errorMessage.isEmpty()) {
                context.buildConstraintViolationWithTemplate(errorMessage.toString()).addConstraintViolation();
                return false;
            }

            return true;
        }
    }
}
