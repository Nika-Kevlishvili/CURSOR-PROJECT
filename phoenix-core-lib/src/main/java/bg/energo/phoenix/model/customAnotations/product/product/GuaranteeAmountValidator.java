package bg.energo.phoenix.model.customAnotations.product.product;

import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.request.product.product.BaseProductRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.util.Set;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {GuaranteeAmountValidator.GuaranteeAmountValidatorImpl.class})
public @interface GuaranteeAmountValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class GuaranteeAmountValidatorImpl implements ConstraintValidator<GuaranteeAmountValidator, BaseProductRequest> {
        @Override
        public boolean isValid(BaseProductRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationViolations = new StringBuilder();

            Set<PaymentGuarantee> paymentGuarantees = request.getPaymentGuarantees();
            if (CollectionUtils.isNotEmpty(paymentGuarantees)) {
                BigDecimal cashDepositAmount = request.getCashDepositAmount();
                Long cashDepositCurrencyId = request.getCashDepositCurrencyId();
                BigDecimal bankGuaranteeAmount = request.getBankGuaranteeAmount();
                Long bankGuaranteeCurrencyId = request.getBankGuaranteeCurrencyId();

                // if only one payment guarantee is selected, and it is NO_PAYMENT_GUARANTEE, then all other fields should be null
                if (paymentGuarantees.size() == 1 && paymentGuarantees.contains(PaymentGuarantee.NO)) {
                    if (cashDepositAmount != null || cashDepositCurrencyId != null || bankGuaranteeAmount != null || bankGuaranteeCurrencyId != null) {
                        context.buildConstraintViolationWithTemplate("You can not fill Cash Deposit or Bank Guarantee amounts and currencies when only [NO_PAYMENT_GUARANTEE] is selected;").addConstraintViolation();
                        return false;
                    }
                }

                // if payment guarantees do not contain BANK_GUARANTEE or CASH_DEPOSIT_AND_BANK, then bank guarantee amount and currency should be null
                if (!CollectionUtils.containsAny(paymentGuarantees, PaymentGuarantee.BANK, PaymentGuarantee.CASH_DEPOSIT_AND_BANK)) {
                    if (bankGuaranteeAmount != null || bankGuaranteeCurrencyId != null) {
                        validationViolations.append("Bank Guarantee Amount & Currency should not be provided when [BANK_GUARANTEE or CASH_DEPOSIT_AND_BANK] is not selected;");
                        isValid = false;
                    }
                } else {
                    // if bank guarantee amount is specified, then bank guarantee currency should be specified as well
                    if (bankGuaranteeAmount != null && bankGuaranteeCurrencyId == null) {
                        validationViolations.append("Bank Guarantee Currency should be provided when Bank Guarantee Amount is specified;");
                        isValid = false;
                    }
                }

                // if payment guarantees do not contain CASH_DEPOSIT or CASH_DEPOSIT_AND_BANK, then cash deposit amount and currency should be null
                if (!CollectionUtils.containsAny(paymentGuarantees, PaymentGuarantee.CASH_DEPOSIT, PaymentGuarantee.CASH_DEPOSIT_AND_BANK)) {
                    if (cashDepositAmount != null || cashDepositCurrencyId != null) {
                        validationViolations.append("Cash Deposit Amount & Currency should not be provided when [CASH_DEPOSIT or CASH_DEPOSIT_AND_BANK] is not selected;");
                        isValid = false;
                    }
                } else {
                    // if cash deposit amount is specified, then cash deposit currency should be specified as well
                    if (cashDepositAmount != null && cashDepositCurrencyId == null) {
                        validationViolations.append("Cash Deposit Currency should be provided when Cash Deposit Amount is specified;");
                        isValid = false;
                    }
                }
            }

            if (!isValid) {
                context.buildConstraintViolationWithTemplate(validationViolations.toString()).addConstraintViolation();
            }

            return isValid;
        }
    }
}
