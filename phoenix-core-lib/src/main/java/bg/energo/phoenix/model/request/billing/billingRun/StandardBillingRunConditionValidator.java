package bg.energo.phoenix.model.request.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.ApplicationModelType;
import bg.energo.phoenix.model.enums.billing.billings.BillingCriteria;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = StandardBillingRunConditionValidator.StandardBillingRunConditionValidatorImpl.class)
public @interface StandardBillingRunConditionValidator {
    String value() default "";

    String message() default "{value}-Invalid Format or symbols;";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class StandardBillingRunConditionValidatorImpl implements ConstraintValidator<StandardBillingRunConditionValidator, StandardBillingParameters> {
        @Override
        public boolean isValid(StandardBillingParameters request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessageBuilder = new StringBuilder();
            // StandardBillingParameters basicParameters = request.getBasicParameters();
            if (request != null) {
                if (request.getBillingCriteria() != null) {
                    if (request.getBillingCriteria().equals(BillingCriteria.CUSTOMERS_CONTRACTS_OR_POD_CONDITIONS)) {
                        if (StringUtils.isEmpty(request.getCustomersContractOrPODConditions())) {
                            validationMessageBuilder.append("basicParameters.customersContractOrPODConditions-[customersContractOrPODConditions] can't be empty when Billing criteria is CUSTOMERS_CONTRACTS_OR_POD_CONDITIONS;");
                        }
                        if (request.getBillingApplicationLevel() == null) {
                            validationMessageBuilder.append("basicParameters.billingApplicationLevel-[billingApplicationLevel] can't be empty when Billing criteria is CUSTOMERS_CONTRACTS_OR_POD_CONDITIONS;");
                        }
                        if (StringUtils.isNotEmpty(request.getListOfCustomersContractsOrPOD())) {
                            validationMessageBuilder.append("basicParameters.listOfCustomersContractsOrPOD-[listOfCustomersContractsOrPOD] should be empty when Billing criteria is CUSTOMERS_CONTRACTS_OR_POD_CONDITIONS;");
                        }
                    } else if (request.getBillingCriteria().equals(BillingCriteria.LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS)) {
                        if (StringUtils.isEmpty(request.getListOfCustomersContractsOrPOD())) {
                            validationMessageBuilder.append("basicParameters.listOfCustomersContractsOrPOD-[listOfCustomersContractsOrPOD] can't be empty when Billing criteria is LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS;");
                        }
                        if (request.getBillingApplicationLevel() == null) {
                            validationMessageBuilder.append("basicParameters.billingApplicationLevel-[billingApplicationLevel] can't be empty when Billing criteria is LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS;");
                        }
                        if (!StringUtils.isEmpty(request.getCustomersContractOrPODConditions())) {
                            validationMessageBuilder.append("basicParameters.customersContractOrPODConditions-[customersContractOrPODConditions] should be null when Billing criteria is LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS;");
                        }
                    } else if (request.getBillingCriteria().equals(BillingCriteria.ALL_CUSTOMERS)) {
                        //application level no longer visible
                        if (request.getBillingApplicationLevel() != null) {
                            validationMessageBuilder.append("basicParameters.billingApplicationLevel-[billingApplicationLevel] should be empty when criteria is ALL_CUSTOMERS;");
                        }
                        if (!StringUtils.isEmpty(request.getCustomersContractOrPODConditions())) {
                            validationMessageBuilder.append("basicParameters.customersContractOrPODConditions-[customersContractOrPODConditions] should be empty when criteria is ALL_CUSTOMERS;");
                        }
                        if (!StringUtils.isEmpty(request.getListOfCustomersContractsOrPOD())) {
                            validationMessageBuilder.append("basicParameters.listOfCustomersContractsOrPOD-[listOfCustomersContractsOrPOD] should be empty when criteria is ALL_CUSTOMERS;");
                        }
                    }
                }
                if (request.getApplicationModelType() != null) {
                    List<ApplicationModelType> applicationModelType = request.getApplicationModelType();
                    if (applicationModelType.contains(ApplicationModelType.WITH_ELECTRICITY_INVOICE) && !applicationModelType.contains(ApplicationModelType.FOR_VOLUMES)) {
                        validationMessageBuilder.append("basicParameters.applicationModelType-[applicationModelType] \"For Volumes\" should be checked with \"With Electricity Invoice\";");
                    }
                }

            }
            if (!validationMessageBuilder.isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessageBuilder.toString()).addConstraintViolation();
                isValid = false;
            }

            return isValid;
        }

    }
}
