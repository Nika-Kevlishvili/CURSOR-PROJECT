package bg.energo.phoenix.model.customAnotations.billing.billingRun;

import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.request.billing.billingRun.iap.InterimAndAdvancePaymentParameters;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.*;
import java.util.regex.Matcher;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
@Constraint(validatedBy = InterimAndAdvancePaymentParametersValidator.InterimAndAdvancePaymentParametersValidation.class)
public @interface InterimAndAdvancePaymentParametersValidator {

    String value() default "";
    String message() default "";
    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


    class InterimAndAdvancePaymentParametersValidation implements ConstraintValidator<InterimAndAdvancePaymentParametersValidator, InterimAndAdvancePaymentParameters> {
        @Override
        public boolean isValid(InterimAndAdvancePaymentParameters parameter, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessages = new StringBuilder();

            if(parameter!=null) {
                if(parameter.getContractId() == null) {
                    if (!parameter.isCostCenterControllingOrderManual()) {
                        validationMessages.append("interimAdvanceAndPaymentParameters.costCenterControllingOrderManual-[costCenterControllingOrderManual] cost center controlling order must be filled manually;");
                    }
                    if (!parameter.isNumberOfIncomeAccountManual()) {
                        validationMessages.append("interimAdvanceAndPaymentParameters.numberOfIncomeAccountManual-[numberOfIncomeAccountManual] number of income account must be filled manually;");
                    }
                    if (!parameter.isApplicableInterestRateManual()) {
                        validationMessages.append("interimAdvanceAndPaymentParameters.applicableInterestRateManual-[applicableInterestRateManual] applicable interest rate must be filled manually;");
                    }
                    if(!parameter.isVatRateManual()) {
                        validationMessages.append("interimAdvanceAndPaymentParameters.vatRateManual-[vatRateManual] Vat Rate must be filled manually;");
                    }
                    if(parameter.getPrefixType() == null) {
                        validationMessages.append("interimAdvanceAndPaymentParameters.prefixType-[prefixType] Prefix Type must be selected when contract/order is not added;");
                    }
                } else {
                    if(parameter.getPrefixType() != null) {
                        validationMessages.append("interimAdvanceAndPaymentParameters.prefixType-[prefixType] Prefix Type must not be selected when contract/order is added;");
                    }
                }
                if(parameter.getNumberOfIncomeAccount()==null && parameter.isNumberOfIncomeAccountManual()) {
                    validationMessages.append("interimAdvanceAndPaymentParameters.numberOfIncomeAccount-[numberOfIncomeAccount] number of income account is mandatory;");
                } else if(parameter.getNumberOfIncomeAccount()!=null && !parameter.isNumberOfIncomeAccountManual()) {
                    validationMessages.append("interimAdvanceAndPaymentParameters.numberOfIncomeAccount-[numberOfIncomeAccount] number of income account is disabled;");
                }
                if(parameter.getCostCenterControllingOrder()==null && parameter.isCostCenterControllingOrderManual()) {
                    validationMessages.append("interimAdvanceAndPaymentParameters.costCenterControllingOrder-[costCenterControllingOrder] cost center controlling order is mandatory;");
                } else if(parameter.getCostCenterControllingOrder()!=null && !parameter.isCostCenterControllingOrderManual()) {
                    validationMessages.append("interimAdvanceAndPaymentParameters.costCenterControllingOrder-[costCenterControllingOrder] cost center controlling order is disabled;");
                }
                if(parameter.getVatRateId()==null && parameter.isVatRateManual() && !parameter.isGlobalVatRate()) {
                    validationMessages.append("interimAdvanceAndPaymentParameters.vatRateId-[vatRateId] vat rate is mandatory;");
                } else if((parameter.getVatRateId()!=null || parameter.isGlobalVatRate()) && !parameter.isVatRateManual()) {
                    validationMessages.append("interimAdvanceAndPaymentParameters.vatRateId-[vatRateId] vat rate is disabled;");
                }

                if(parameter.getApplicableInterestRateId()==null && parameter.isApplicableInterestRateManual()) {
                    validationMessages.append("interimAdvanceAndPaymentParameters.applicableInterestRateId-[applicableInterestRateId] applicable interest rate is mandatory;");

                } else if(parameter.getApplicableInterestRateId()!=null && !parameter.isApplicableInterestRateManual()) {
                    validationMessages.append("interimAdvanceAndPaymentParameters.applicableInterestRateId-[applicableInterestRateId] applicable interest rate is disabled;");
                }

                if(parameter.isDirectDebit()) {
                    if(!parameter.isDirectDebitManual()) {
                        validationMessages.append("interimAdvanceAndPaymentParameters.directDebit-[directDebit] direct debit is disabled;");
                    }
                    if(parameter.getBankId()==null) {
                        validationMessages.append("interimAdvanceAndPaymentParameters.bankId-[bankId] bank id is mandatory;");
                    }

                    if(StringUtils.isEmpty(parameter.getIban())) {
                        validationMessages.append("interimAdvanceAndPaymentParameters.iban-[iban] iban is mandatory;");
                    }else {
                        String value = parameter.getIban();
                        if (StringUtils.isNotEmpty(value)) {
                            if (value.startsWith("BG")) {
                                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^BG\\d{2}[A-Z]{4}\\d{6}[A-Z\\d]{8}$");
                                Matcher matcher = pattern.matcher(value);
                                if (!matcher.matches()) {
                                    validationMessages.append("interimAndAdvancePaymentParameters.iban-[iban] iban must be valid;");
                                }
                            } else {
                                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^[A-Z]{2}[A-Z\\d]{12,32}$");
                                Matcher matcher = pattern.matcher(value);
                                if (value.length() < 14 || value.length() > 34 || !matcher.matches()) {
                                    validationMessages.append("interimAndAdvancePaymentParameters.iban-[iban] iban must be valid;");
                                }
                            }
                        }
                    }

                } else {
                    if(parameter.getBankId()!=null) {
                        validationMessages.append("interimAdvanceAndPaymentParameters.bankId-[bankId] bank id is disabled;");
                    }
                    if(parameter.getIban()!=null) {
                        validationMessages.append("interimAdvanceAndPaymentParameters.iban-[iban] iban is disabled;");
                    }
                }

                if(parameter.getContractType()==null && parameter.getContractId()!=null) {
                    validationMessages.append("interimAdvanceAndPaymentParameters.contractType-[contractType] contract type is mandatory");
                }
                if( parameter.getContractType() == ContractType.PRODUCT_CONTRACT && parameter.getContractId()!=null && (parameter.getBillingGroupIds() == null || parameter.getBillingGroupIds().isEmpty())) {
                    validationMessages.append("interimAdvanceAndPaymentParameters.billingGroupIds-[billingGroupIds] at least one billing group is mandatory");
                } else if(parameter.getContractType()== ContractType.SERVICE_CONTRACT && parameter.getContractId()!=null &&  (parameter.getBillingGroupIds()!=null && !parameter.getBillingGroupIds().isEmpty())) {
                    validationMessages.append("interimAdvanceAndPaymentParameters.billingGroupIds-[billingGroupIds] billing groups is disabled");
                }

            }
            if (!validationMessages.isEmpty()) {
                context.buildConstraintViolationWithTemplate(validationMessages.toString()).addConstraintViolation();
                isValid = false;
            }

            return isValid;
        }
    }
}
