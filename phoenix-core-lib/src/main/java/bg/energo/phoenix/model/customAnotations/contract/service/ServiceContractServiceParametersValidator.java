package bg.energo.phoenix.model.customAnotations.contract.service;

import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.request.contract.service.ServiceContractServiceParametersCreateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ServiceContractServiceParametersValidator.ServiceContractServiceParametersValidatorImpl.class})
public @interface ServiceContractServiceParametersValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ServiceContractServiceParametersValidatorImpl implements ConstraintValidator<ServiceContractServiceParametersValidator, ServiceContractServiceParametersCreateRequest> {
        @Override
        public boolean isValid(ServiceContractServiceParametersCreateRequest request, ConstraintValidatorContext context) {
            BigDecimal quantity = request.getQuantity();
            if(quantity != null){
                if(!(quantity.stripTrailingZeros().scale() <= 0)){
                    context.buildConstraintViolationWithTemplate("paymentGuarantee.quantity-is not a whole number;").addConstraintViolation();
                    return false;
                }
            }
            PaymentGuarantee paymentGuarantee = request.getPaymentGuarantee();
            if (paymentGuarantee != null) {
                if (paymentGuarantee.equals(PaymentGuarantee.CASH_DEPOSIT_AND_BANK)) {
                    if (request.getCashDepositAmount() == null) {
                        context.buildConstraintViolationWithTemplate("paymentGuarantee.cashDepositAmount-is mandatory when CASH_DEPOSIT or CASH_DEPOSIT_AND_BANK is selected;").addConstraintViolation();
                        return false;
                    }
                    if (request.getCashDepositCurrencyId() == null) {
                        context.buildConstraintViolationWithTemplate("paymentGuarantee.cashDepositCurrencyId-is mandatory when CASH_DEPOSIT or CASH_DEPOSIT_AND_BANK is selected;").addConstraintViolation();
                        return false;
                    }
                    if (request.getBankGuaranteeAmount() == null) {
                        context.buildConstraintViolationWithTemplate("paymentGuarantee.bankGuaranteeAmount-is mandatory when CASH_DEPOSIT or BANK is selected;").addConstraintViolation();
                        return false;
                    }
                    if (request.getBankGuaranteeCurrencyId() == null) {
                        context.buildConstraintViolationWithTemplate("paymentGuarantee.bankGuaranteeCurrencyId-is mandatory when CASH_DEPOSIT or CASH_DEPOSIT_AND_BANK is selected;").addConstraintViolation();
                        return false;
                    }
                }

                if (paymentGuarantee.equals(PaymentGuarantee.BANK)) {
                    if (request.getBankGuaranteeAmount() == null) {
                        context.buildConstraintViolationWithTemplate("paymentGuarantee.bankGuaranteeAmount-is mandatory when CASH_DEPOSIT or BANK is selected;").addConstraintViolation();
                        return false;
                    }
                    if (request.getBankGuaranteeCurrencyId() == null) {
                        context.buildConstraintViolationWithTemplate("paymentGuarantee.bankGuaranteeCurrencyId-is mandatory when CASH_DEPOSIT or CASH_DEPOSIT_AND_BANK is selected;").addConstraintViolation();
                        return false;
                    }
                }

                if (paymentGuarantee.equals(PaymentGuarantee.CASH_DEPOSIT)) {
                    if (request.getCashDepositAmount() == null) {
                        context.buildConstraintViolationWithTemplate("paymentGuarantee.cashDepositAmount-is mandatory when CASH_DEPOSIT or CASH_DEPOSIT_AND_BANK is selected;").addConstraintViolation();
                        return false;
                    }
                    if (request.getCashDepositCurrencyId() == null) {
                        context.buildConstraintViolationWithTemplate("paymentGuarantee.cashDepositCurrencyId-is mandatory when CASH_DEPOSIT or CASH_DEPOSIT_AND_BANK is selected;").addConstraintViolation();
                        return false;
                    }
                }
            }
            /*if (request.getGuaranteeContract() == null) {
                context.buildConstraintViolationWithTemplate("guaranteeContract-[guaranteeContract] mustn't be null;").addConstraintViolation();
                return false;
            }*/
            if (request.isGuaranteeContract()) {
                if (StringUtils.isEmpty(request.getGuaranteeContractInfo())) {
                    context.buildConstraintViolationWithTemplate("guaranteeContractInfo-[guaranteeContractInfo] mustn't be null;").addConstraintViolation();
                    return false;
                }
            }
            if (request.getEntryIntoForceDate() != null) {
                if (request.getEntryIntoForce() == null) {
                    context.buildConstraintViolationWithTemplate("serviceParameters.contractEntryIntoForce- mustn't be null;").addConstraintViolation();
                    return false;
                }
            }
            if (request.getStartOfContractInitialTerm() != null) {
                if(request.getStartOfContractInitialTerm().equals(StartOfContractInitialTerm.EXACT_DATE)){
                    if (request.getStartOfContractInitialTermDate() == null) {
                        context.buildConstraintViolationWithTemplate("serviceParameters.startInitialTermOfTheContract- mustn't be null;").addConstraintViolation();
                        return false;
                    }
                }
            }
            /*if(request.getContractTermType().equals(ServiceContractContractTermType.CERTAIN_DATE)){
                if(request.getContractTermEndDate() == null){
                    context.buildConstraintViolationWithTemplate("serviceParameters.contractTermEndDate- mustn't be null when type is certain date;").addConstraintViolation();
                    return false;
                }
            }*/

            return true;
        }
    }
}
