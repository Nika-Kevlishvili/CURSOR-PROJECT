package bg.energo.phoenix.model.customAnotations.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.ManualInvoiceType;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.ManualInvoiceDetailedDataParameters;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.ManualInvoiceSummaryDataParameters;
import bg.energo.phoenix.model.request.billing.billingRun.manualCreditOrDebitNote.ManualCreditOrDebitNoteBasicDataParameters;
import bg.energo.phoenix.model.request.billing.billingRun.manualCreditOrDebitNote.ManualCreditOrDebitNoteParameters;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
@Constraint(validatedBy = ManualCreditOrDebitNoteParametersValidator.ManualCreditOrDebitNoteParametersValidatorImpl.class)
public @interface ManualCreditOrDebitNoteParametersValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


    class ManualCreditOrDebitNoteParametersValidatorImpl implements ConstraintValidator<ManualCreditOrDebitNoteParametersValidator, ManualCreditOrDebitNoteParameters> {
        @Override
        public boolean isValid(ManualCreditOrDebitNoteParameters manualCreditOrDebitNoteParameters, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessages = new StringBuilder();
            ManualCreditOrDebitNoteBasicDataParameters basicDataParameters = manualCreditOrDebitNoteParameters.getManualCreditOrDebitNoteBasicDataParameters();
            validateBasicDataParameters(basicDataParameters, validationMessages);
            validateDetailedDataParameters(manualCreditOrDebitNoteParameters, validationMessages);
            if (!validationMessages.isEmpty()) {
                context.buildConstraintViolationWithTemplate(validationMessages.toString()).addConstraintViolation();
                isValid = false;
            }
            return isValid;
        }

        private void validateDetailedDataParameters(ManualCreditOrDebitNoteParameters manualCreditOrDebitNoteParameters, StringBuilder validationMessages) {
            ManualInvoiceSummaryDataParameters summaryDataParameters = manualCreditOrDebitNoteParameters.getManualCreditOrDebitNoteSummaryDataParameters();
            ManualInvoiceDetailedDataParameters detailedDataParameters = manualCreditOrDebitNoteParameters.getManualCreditOrDebitNoteDetailedDataParameters();
            if (summaryDataParameters != null) {
                ManualInvoiceType summaryDataType = summaryDataParameters.getManualInvoiceType();
                if (summaryDataType != null && summaryDataType.equals(ManualInvoiceType.STANDARD_INVOICE)) {
                    if (detailedDataParameters != null) {
                        validationMessages.append("manualCreditOrDebitNoteParameters.manualCreditOrDebitNoteDetailedDataParameters-[manualCreditOrDebitNoteDetailedDataParameters] should be null;");
                    }
                }
            }
        }

        private void validateBasicDataParameters(ManualCreditOrDebitNoteBasicDataParameters basicDataParameters, StringBuilder validationMessages) {
            if (basicDataParameters.getBillingRunInvoiceInformationList().size() > 1) {
                if (!basicDataParameters.isCostCenterControllingOrderManual()) {
                    validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.costCenterControllingOrderManual-[costCenterControllingOrderManual] cost center controlling order must be filled manually;");
                }
                if (!basicDataParameters.isNumberOfIncomeAccountManual()) {
                    validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.numberOfIncomeAccountManual-[numberOfIncomeAccountManual] number of income account must be filled manually;");
                }
                if (!basicDataParameters.isApplicableInterestRateManual()) {
                    validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.applicableInterestRateManual-[applicableInterestRateManual] applicable interest rate must be filled manually;");
                }
                if (!basicDataParameters.isVatRateManual()) {
                    validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.vatRateManual-[vatRateManual] Vat Rate must be filled manually;");
                }
                if (!basicDataParameters.isDirectDebitManual()) {
                    validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.directDebitManual-[directDebitManual] direct debit must be filled manually;");
                }
            }
            validateNumberOfIncomeAccount(basicDataParameters, validationMessages);
            validateCostCenterControllingOrder(basicDataParameters, validationMessages);
            validateVatRate(basicDataParameters, validationMessages);
            validateApplicableInterestRate(basicDataParameters, validationMessages);
            validateBankParameters(basicDataParameters, validationMessages);
        }

        private void validateBankParameters(ManualCreditOrDebitNoteBasicDataParameters basicDataParameters, StringBuilder validationMessages) {
            if (basicDataParameters.isDirectDebit()) {
                if (!basicDataParameters.isDirectDebitManual()) {
                    validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.directDebit-[directDebit] direct debit is disabled;");
                }
                if (basicDataParameters.getBankId() == null) {
                    validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.bankId-[bankId] bank id is mandatory;");
                }
                if (StringUtils.isEmpty(basicDataParameters.getIban())) {
                    validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.iban-[iban] iban is mandatory;");
                }
            } else {
                if (basicDataParameters.getBankId() != null) {
                    validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.bankId-[bankId] bank id is disabled;");
                }
                if (StringUtils.isNotEmpty(basicDataParameters.getIban())) {
                    validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.iban-[iban] iban is disabled;");
                }
            }
        }

        private void validateApplicableInterestRate(ManualCreditOrDebitNoteBasicDataParameters basicDataParameters, StringBuilder validationMessages) {
            if (basicDataParameters.getApplicableInterestRateId() == null && basicDataParameters.isApplicableInterestRateManual()) {
                validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.applicableInterestRateId-[applicableInterestRateId] applicable interest rate is mandatory;");
            } else if (basicDataParameters.getApplicableInterestRateId() != null && !basicDataParameters.isApplicableInterestRateManual()) {
                validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.applicableInterestRateId-[applicableInterestRateId] applicable interest rate is disabled;");
            }
        }

        private void validateVatRate(ManualCreditOrDebitNoteBasicDataParameters basicDataParameters, StringBuilder validationMessages) {
            if (basicDataParameters.getVatRateId() == null && basicDataParameters.isVatRateManual() && !basicDataParameters.isGlobalVatRate()) {
                validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.vatRateId-[vatRateId] vat rate is mandatory;");
            } else if ((basicDataParameters.getVatRateId() != null || basicDataParameters.isGlobalVatRate()) && !basicDataParameters.isVatRateManual()) {
                validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.vatRateId-[vatRateId] vat rate is disabled;");
            }
        }

        private void validateCostCenterControllingOrder(ManualCreditOrDebitNoteBasicDataParameters basicDataParameters, StringBuilder validationMessages) {
            if (basicDataParameters.getCostCenterControllingOrder() == null && basicDataParameters.isCostCenterControllingOrderManual()) {
                validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.costCenterControllingOrder-[costCenterControllingOrder] cost center controlling order is mandatory;");
            } else if (basicDataParameters.getCostCenterControllingOrder() != null && !basicDataParameters.isCostCenterControllingOrderManual()) {
                validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.costCenterControllingOrder-[costCenterControllingOrder] cost center controlling order is disabled;");
            }
        }

        private void validateNumberOfIncomeAccount(ManualCreditOrDebitNoteBasicDataParameters basicDataParameters, StringBuilder validationMessages) {
            if (basicDataParameters.getNumberOfIncomeAccount() == null && basicDataParameters.isNumberOfIncomeAccountManual()) {
                validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.numberOfIncomeAccount-[numberOfIncomeAccount] number of income account is mandatory;");
            } else if (basicDataParameters.getNumberOfIncomeAccount() != null && !basicDataParameters.isNumberOfIncomeAccountManual()) {
                validationMessages.append("manualCreditOrDebitNoteBasicDataParameters.numberOfIncomeAccount-[numberOfIncomeAccount] number of income account is disabled;");
            }
        }

    }
}
