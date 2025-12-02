package bg.energo.phoenix.model.customAnotations.contract.products;

import bg.energo.phoenix.model.request.contract.product.additionalParameters.ProductContractBankingDetails;
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
@Constraint(validatedBy = {ValidProductContractBankingDetails.ProductContractBankingDetailsValidator.class})
public @interface ValidProductContractBankingDetails {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ProductContractBankingDetailsValidator implements ConstraintValidator<ValidProductContractBankingDetails, ProductContractBankingDetails> {

        @Override
        public boolean isValid(ProductContractBankingDetails bankingDetails, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();

            if (bankingDetails.getDirectDebit() == null) {
                context.buildConstraintViolationWithTemplate("additionalParameters.bankingDetails.directDebit-Direct Debit is mandatory;").addConstraintViolation();
                return false;
            }

            StringBuilder errorMessage = new StringBuilder();

            if (bankingDetails.getDirectDebit()) {
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
