package bg.energo.phoenix.model.customAnotations.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.BillingCriteria;
import bg.energo.phoenix.model.request.billing.billingRun.StandardBillingParameters;
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
@Constraint(validatedBy = BillingRunBasicParametersValidator.BillingRunBasicParametersValidatorImpl.class)
public @interface BillingRunBasicParametersValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
    class BillingRunBasicParametersValidatorImpl implements ConstraintValidator<BillingRunBasicParametersValidator, StandardBillingParameters> {

        @Override
        public boolean isValid(StandardBillingParameters basicParameters, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessageBuilder = new StringBuilder();
            if(basicParameters!=null) {
                if (basicParameters.getBillingCriteria() != null) {
                    if (basicParameters.getBillingCriteria().equals(BillingCriteria.CUSTOMERS_CONTRACTS_OR_POD_CONDITIONS)) {
                        if (StringUtils.isEmpty(basicParameters.getCustomersContractOrPODConditions())) {
                            validationMessageBuilder.append("basicParameters.customersContractOrPODConditions-[customersContractOrPODConditions] can't be empty when Billing criteria is CUSTOMERS_CONTRACTS_OR_POD_CONDITIONS;");
                        }
                        if (basicParameters.getBillingApplicationLevel() == null) {
                            validationMessageBuilder.append("basicParameters.billingApplicationLevel-[billingApplicationLevel] can't be empty when Billing criteria is CUSTOMERS_CONTRACTS_OR_POD_CONDITIONS;");
                        }
                    }

                    if (basicParameters.getBillingCriteria().equals(BillingCriteria.LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS)) {
                        if (StringUtils.isEmpty(basicParameters.getListOfCustomersContractsOrPOD())) {
                            validationMessageBuilder.append("basicParameters.listOfCustomersContractsOrPOD-[listOfCustomersContractsOrPOD] can't be empty when Billing criteria is LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS;");
                        }
                        if (basicParameters.getBillingApplicationLevel() == null) {
                            validationMessageBuilder.append("basicParameters.billingApplicationLevel-[billingApplicationLevel] can't be empty when Billing criteria is LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS;");
                        }
                    }
                    if (basicParameters.getBillingCriteria().equals(BillingCriteria.ALL_CUSTOMERS)) {
                        if (basicParameters.getBillingApplicationLevel() != null) {
                            validationMessageBuilder.append("basicParameters.billingApplicationLevel-[billingApplicationLevel] should be empty when criteria is ALL_CUSTOMERS;");
                        }
                        if (!StringUtils.isEmpty(basicParameters.getCustomersContractOrPODConditions())) {
                            validationMessageBuilder.append("basicParameters.customersContractOrPODConditions-[customersContractOrPODConditions] should be empty when criteria is ALL_CUSTOMERS;");
                        }
                        if (!StringUtils.isEmpty(basicParameters.getListOfCustomersContractsOrPOD())) {
                            validationMessageBuilder.append("basicParameters.listOfCustomersContractsOrPOD-[listOfCustomersContractsOrPOD] should be empty when criteria is ALL_CUSTOMERS;");
                        }
                    }
                }
            }
            if (!validationMessageBuilder.isEmpty()) {
                context.buildConstraintViolationWithTemplate(validationMessageBuilder.toString()).addConstraintViolation();
                isValid = false;
            }

            return isValid;
        }
    }
}
