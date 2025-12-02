package bg.energo.phoenix.model.customAnotations.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.ContractOrderType;
import bg.energo.phoenix.model.enums.billing.billings.ManualInvoiceType;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.ManualInvoiceBasicDataParameters;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.ManualInvoiceDetailedDataParameters;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.ManualInvoiceParameters;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.ManualInvoiceSummaryDataParameters;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
@Constraint(validatedBy = ManualInvoiceParametersValidator.ManualInvoiceParametersValidatorImpl.class)
public @interface ManualInvoiceParametersValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


    class ManualInvoiceParametersValidatorImpl implements ConstraintValidator<ManualInvoiceParametersValidator, ManualInvoiceParameters> {
        @Override
        public boolean isValid(ManualInvoiceParameters manualInvoiceParameters, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessages = new StringBuilder();
            ManualInvoiceBasicDataParameters basicDataParameters = manualInvoiceParameters.getManualInvoiceBasicDataParameters();
            validateBasicDataParameters(basicDataParameters, validationMessages);
            validateDetailedDataParameters(manualInvoiceParameters, validationMessages);
            if (!validationMessages.isEmpty()) {
                context.buildConstraintViolationWithTemplate(validationMessages.toString()).addConstraintViolation();
                isValid = false;
            }
            return isValid;
        }

        private void validateDetailedDataParameters(ManualInvoiceParameters manualInvoiceParameters, StringBuilder validationMessages) {
            ManualInvoiceSummaryDataParameters summaryDataParameters = manualInvoiceParameters.getManualInvoiceSummaryDataParameters();
            ManualInvoiceDetailedDataParameters detailedDataParameters = manualInvoiceParameters.getManualInvoiceDetailedDataParameters();
            if (summaryDataParameters != null) {
                ManualInvoiceType summaryDataType = summaryDataParameters.getManualInvoiceType();
                if (summaryDataType != null && summaryDataType.equals(ManualInvoiceType.STANDARD_INVOICE)) {
                    if (detailedDataParameters != null) {
                        validationMessages.append("manualInvoiceParameters.manualInvoiceDetailedDataParameters-[manualInvoiceDetailedDataParameters] should be null;");
                    }
                }
            }
        }

        private void validateBasicDataParameters(ManualInvoiceBasicDataParameters basicDataParameters, StringBuilder validationMessages) {
            if (basicDataParameters.getContractOrderId() == null) {
                if (!basicDataParameters.isCostCenterControllingOrderManual()) {
                    validationMessages.append("manualInvoiceBasicDataParameters.costCenterControllingOrderManual-[costCenterControllingOrderManual] cost center controlling order must be filled manually;");
                }
                if (!basicDataParameters.isNumberOfIncomeAccountManual()) {
                    validationMessages.append("manualInvoiceBasicDataParameters.numberOfIncomeAccountManual-[numberOfIncomeAccountManual] number of income account must be filled manually;");
                }
                if (!basicDataParameters.isApplicableInterestRateManual()) {
                    validationMessages.append("manualInvoiceBasicDataParameters.applicableInterestRateManual-[applicableInterestRateManual] applicable interest rate must be filled manually;");
                }
                if (!basicDataParameters.isVatRateManual()) {
                    validationMessages.append("manualInvoiceBasicDataParameters.vatRateManual-[vatRateManual] Vat Rate must be filled manually;");
                }
                if (basicDataParameters.getPrefixType() == null) {
                    validationMessages.append("manualInvoiceBasicDataParameters.prefixType-[prefixType] Prefix Type must be selected when contract/order is not added;");
                }
            } else {
                if (basicDataParameters.getPrefixType() != null) {
                    validationMessages.append("manualInvoiceBasicDataParameters.prefixType-[prefixType] Prefix Type must not be selected when contract/order is added;");
                }
            }
            validateNumberOfIncomeAccount(basicDataParameters, validationMessages);
            validateCostCenterControllingOrder(basicDataParameters, validationMessages);
            validateVatRate(basicDataParameters, validationMessages);
            validateApplicableInterestRate(basicDataParameters, validationMessages);
            validateBankParameters(basicDataParameters, validationMessages);
            validateContractOrder(basicDataParameters, validationMessages);

        }

        private void validateContractOrder(ManualInvoiceBasicDataParameters basicDataParameters, StringBuilder validationMessages) {
            if (basicDataParameters.getContractOrderType() == null && basicDataParameters.getContractOrderId() != null) {
                validationMessages.append("manualInvoiceBasicDataParameters.contractOrderType-[contractOrderType] contract order type is mandatory;");
            }
            if (basicDataParameters.getContractOrderType() == ContractOrderType.PRODUCT_CONTRACT) {
                if (basicDataParameters.getBillingGroupId() == null) {
                    validationMessages.append("manualInvoiceBasicDataParameters.billingGroupId-[billingGroupId] billing group is mandatory;");
                }
            } else if (basicDataParameters.getBillingGroupId() != null) {
                validationMessages.append("manualInvoiceBasicDataParameters.billingGroupId-[billingGroupId] billing group is disabled;");
            }
        }

        private void validateBankParameters(ManualInvoiceBasicDataParameters basicDataParameters, StringBuilder validationMessages) {
            if (basicDataParameters.isDirectDebit()) {
                if (!basicDataParameters.isDirectDebitManual()) {
                    validationMessages.append("manualInvoiceBasicDataParameters.directDebit-[directDebit] direct debit is disabled;");
                }
                if (basicDataParameters.getBankId() == null) {
                    validationMessages.append("manualInvoiceBasicDataParameters.bankId-[bankId] bank id is mandatory;");
                }
                if (StringUtils.isEmpty(basicDataParameters.getIban())) {
                    validationMessages.append("manualInvoiceBasicDataParameters.iban-[iban] iban is mandatory;");
                }
            } else {
                if (basicDataParameters.getBankId() != null) {
                    validationMessages.append("manualInvoiceBasicDataParameters.bankId-[bankId] bank id is disabled;");
                }
                if (StringUtils.isNotEmpty(basicDataParameters.getIban())) {
                    validationMessages.append("manualInvoiceBasicDataParameters.iban-[iban] iban is disabled;");
                }
            }
        }

        private void validateApplicableInterestRate(ManualInvoiceBasicDataParameters basicDataParameters, StringBuilder validationMessages) {
            if (basicDataParameters.getApplicableInterestRateId() == null && basicDataParameters.isApplicableInterestRateManual()) {
                validationMessages.append("manualInvoiceBasicDataParameters.applicableInterestRateId-[applicableInterestRateId] applicable interest rate is mandatory;");
            } else if (basicDataParameters.getApplicableInterestRateId() != null && !basicDataParameters.isApplicableInterestRateManual()) {
                validationMessages.append("manualInvoiceBasicDataParameters.applicableInterestRateId-[applicableInterestRateId] applicable interest rate is disabled;");
            }
        }

        private void validateVatRate(ManualInvoiceBasicDataParameters basicDataParameters, StringBuilder validationMessages) {
            if (basicDataParameters.getVatRateId() == null && basicDataParameters.isVatRateManual() && !basicDataParameters.isGlobalVatRate()) {
                validationMessages.append("manualInvoiceBasicDataParameters.vatRateId-[vatRateId] vat rate is mandatory;");
            } else if ((basicDataParameters.getVatRateId() != null || basicDataParameters.isGlobalVatRate()) && !basicDataParameters.isVatRateManual()) {
                validationMessages.append("manualInvoiceBasicDataParameters.vatRateId-[vatRateId] vat rate is disabled;");
            }
        }

        private void validateCostCenterControllingOrder(ManualInvoiceBasicDataParameters basicDataParameters, StringBuilder validationMessages) {
            if (basicDataParameters.getCostCenterControllingOrder() == null && basicDataParameters.isCostCenterControllingOrderManual()) {
                validationMessages.append("manualInvoiceBasicDataParameters.costCenterControllingOrder-[costCenterControllingOrder] cost center controlling order is mandatory;");
            } else if (basicDataParameters.getCostCenterControllingOrder() != null && !basicDataParameters.isCostCenterControllingOrderManual()) {
                validationMessages.append("manualInvoiceBasicDataParameters.costCenterControllingOrder-[costCenterControllingOrder] cost center controlling order is disabled;");
            }
        }

        private void validateNumberOfIncomeAccount(ManualInvoiceBasicDataParameters basicDataParameters, StringBuilder validationMessages) {
            if (basicDataParameters.getNumberOfIncomeAccount() == null && basicDataParameters.isNumberOfIncomeAccountManual()) {
                validationMessages.append("manualInvoiceBasicDataParameters.numberOfIncomeAccount-[numberOfIncomeAccount] number of income account is mandatory;");
            } else if (basicDataParameters.getNumberOfIncomeAccount() != null && !basicDataParameters.isNumberOfIncomeAccountManual()) {
                validationMessages.append("manualInvoiceBasicDataParameters.numberOfIncomeAccount-[numberOfIncomeAccount] number of income account is disabled;");
            }
        }


    }
}
