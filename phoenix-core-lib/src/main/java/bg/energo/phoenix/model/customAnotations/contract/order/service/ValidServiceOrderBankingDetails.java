package bg.energo.phoenix.model.customAnotations.contract.order.service;

import bg.energo.phoenix.model.request.contract.order.service.ServiceOrderBankingDetails;
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
@Constraint(validatedBy = {ValidServiceOrderBankingDetails.ServiceOrderBankingDetailsValidator.class})
public @interface ValidServiceOrderBankingDetails {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ServiceOrderBankingDetailsValidator implements ConstraintValidator<ValidServiceOrderBankingDetails, ServiceOrderBankingDetails> {

        @Override
        public boolean isValid(ServiceOrderBankingDetails bankingDetails, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();

            StringBuilder errorMessage = new StringBuilder();

            if (Boolean.TRUE.equals(bankingDetails.getDirectDebit())) {
                if (bankingDetails.getBankId() == null) {
                    errorMessage.append("additionalParameters.bankingDetails.bankId-Bank is mandatory when direct debit is selected;");
                }

                if (StringUtils.isEmpty(bankingDetails.getIban())) {
                    errorMessage.append("additionalParameters.bankingDetails.iban-IBAN is mandatory when direct debit is selected;");
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
