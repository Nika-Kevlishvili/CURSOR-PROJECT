package bg.energo.phoenix.model.customAnotations.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.*;
import bg.energo.phoenix.model.request.billing.billingRun.BillingRunCommonParameters;
import bg.energo.phoenix.model.request.billing.billingRun.StandardBillingParameters;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.ManualInvoiceBasicDataParameters;
import bg.energo.phoenix.model.request.billing.billingRun.edit.BillingRunEditRequest;
import bg.energo.phoenix.model.request.billing.billingRun.edit.manualInvoice.ManualInvoiceEditParameters;
import bg.energo.phoenix.model.request.billing.billingRun.iap.InterimAndAdvancePaymentParameters;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = BillingRunEditBaseRequestValidator.BillingRunEditBaseRequestValidatorImpl.class)
public @interface BillingRunEditBaseRequestValidator {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class BillingRunEditBaseRequestValidatorImpl implements ConstraintValidator<BillingRunEditBaseRequestValidator, BillingRunEditRequest> {
        @Override
        public boolean isValid(BillingRunEditRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessageBuilder = new StringBuilder();
            BillingRunCommonParameters commonParameters = request.getCommonParameters();
            if (commonParameters != null) {
                if (Objects.equals(request.getBillingType(), BillingType.MANUAL_INVOICE)) {
                    if (Objects.equals(commonParameters.getPeriodicity(), BillingRunPeriodicity.PERIODIC)) {
                        validationMessageBuilder.append("commonParameters.periodicity-[periodicity] should be STANDARD for manual invoice;");
                    }
                }
                if (Objects.equals(request.getBillingType(), BillingType.INVOICE_REVERSAL)) {
                    if (request.getInvoiceReversalParameters() == null) {
                        validationMessageBuilder.append("invoiceReversalParameters-invoiceReversalParameters can not be null;");
                    }

                    if (commonParameters.getAccountingPeriodId() == null) {
                        validationMessageBuilder.append("commonParameters.accountingPeriodId-[accountingPeriodId] should be present;");
                    }
                    if (commonParameters.getInvoiceDate() == null) {
                        validationMessageBuilder.append("commonParameters.invoiceDate-[invoiceDate] should be present;");
                    }

                    LocalDateTime executionDateAndTime = commonParameters.getExecutionDateAndTime();
                    if (executionDateAndTime != null && executionDateAndTime.isBefore(LocalDateTime.now())) {
                        validationMessageBuilder.append("executionDateAndTime-executionDateAndTime can not be the past;");
                    }

                    ExecutionType executionType = commonParameters.getExecutionType();
                    if (executionType == null) {
                        validationMessageBuilder.append("executionType-executionType is mandatory!;");
                    } else if (executionType.equals(ExecutionType.EXACT_DATE) && commonParameters.getExecutionDateAndTime() == null) {
                        validationMessageBuilder.append("executionDateAndTime-Execution date and time is mandatory if Exact date is selected;");
                    } else if ((executionType.equals(ExecutionType.IMMEDIATELY) || executionType.equals(ExecutionType.MANUAL)) && commonParameters.getExecutionDateAndTime() != null) {
                        validationMessageBuilder.append("executionDateAndTime-Execution date and time must be null when Immediate or Manual is selected;");
                    }
                }
                if (Objects.equals(request.getBillingType(), BillingType.INVOICE_CORRECTION)) {
                    if (request.getInvoiceCorrectionParameters() == null) {
                        validationMessageBuilder.append("invoiceCorrectionParameters-invoiceCorrectionParameters can not be null;");
                    }

                    LocalDateTime executionDateAndTime = commonParameters.getExecutionDateAndTime();
                    if (executionDateAndTime != null && executionDateAndTime.isBefore(LocalDateTime.now())) {
                        validationMessageBuilder.append("executionDateAndTime-executionDateAndTime can not be the past;");
                    }

                    ExecutionType executionType = commonParameters.getExecutionType();
                    if (executionType == null) {
                        validationMessageBuilder.append("executionType-executionType is mandatory!;");
                    } else if (executionType.equals(ExecutionType.EXACT_DATE) && commonParameters.getExecutionDateAndTime() == null) {
                        validationMessageBuilder.append("executionDateAndTime-Execution date and time is mandatory if Exact date is selected;");
                    } else if ((executionType.equals(ExecutionType.IMMEDIATELY) || executionType.equals(ExecutionType.MANUAL)) && commonParameters.getExecutionDateAndTime() != null) {
                        validationMessageBuilder.append("executionDateAndTime-Execution date and time must be null when Immediate or Manual is selected;");
                    }

                    InvoiceDueDateType invoiceDueDate = commonParameters.getInvoiceDueDate();
                    if (invoiceDueDate == null) {
                        validationMessageBuilder.append("invoiceDueDate-invoiceDueDate is mandatory;");
                    } else if (invoiceDueDate.equals(InvoiceDueDateType.DATE) && commonParameters.getDueDate() == null) {
                        validationMessageBuilder.append("dueDate-dueDate is mandatory when Due date type is DATE;");
                    } else if (invoiceDueDate.equals(InvoiceDueDateType.ACCORDING_TO_THE_CONTRACT) && commonParameters.getDueDate() != null) {
                        validationMessageBuilder.append("dueDate-dueDate must be null when Due date type is According to the Contract;");
                    }

                    if (commonParameters.getDueDate() != null && commonParameters.getInvoiceDate() != null && commonParameters.getDueDate().isBefore(commonParameters.getInvoiceDate())) {
                        validationMessageBuilder.append("dueDate-dueDate can not be before invoice Date;");
                    }
                }
                if (commonParameters.getPeriodicity() == null || commonParameters.getPeriodicity().equals(BillingRunPeriodicity.STANDARD)) {
                    if (Objects.equals(request.getBillingType(), BillingType.STANDARD_BILLING)) {
                        checkStandardBillingMaxBilling(request, commonParameters, validationMessageBuilder);
                    }
                    if (commonParameters.getAccountingPeriodId() == null) {
                        validationMessageBuilder.append("commonParameters.accountingPeriodId-[accountingPeriodId] should be present when billing type is STANDARD_BILLING;");
                    }
                    if (commonParameters.getTaxEventDate() == null && !request.getBillingType().equals(BillingType.INVOICE_REVERSAL)) {
                        validationMessageBuilder.append("commonParameters.taxEventDate-[taxEventDate] should be present when billing type is STANDARD_BILLING;");
                    }
                    if (commonParameters.getInvoiceDate() == null) {
                        validationMessageBuilder.append("commonParameters.invoiceDate-[invoiceDate] should be present when billing type is STANDARD_BILLING;");
                    }
                    if (!CollectionUtils.isEmpty(commonParameters.getProcessPeriodicityIds())) {
                        validationMessageBuilder.append("commonParameters.processPeriodicityIds-[processPeriodicityIds] should be empty;");
                    }
                    if (commonParameters.getExecutionType() == null) {
                        validationMessageBuilder.append("commonParameters.executionType-[executionType] shouldn't be empty when periodicity is STANDARD;");
                    }
                    if (Objects.equals(commonParameters.getInvoiceDueDate(), InvoiceDueDateType.DATE)) {
                        if (commonParameters.getDueDate() == null) {
                            validationMessageBuilder.append("commonParameters.dueDate-[dueDate] when invoiceDueDateType is DATE dueDate is mandatory;");
                        }
                        if (commonParameters.getInvoiceDate() != null && !commonParameters.getInvoiceDate().isBefore(commonParameters.getDueDate()) && (!commonParameters.getInvoiceDate().equals(commonParameters.getDueDate()))) {
                            validationMessageBuilder.append("commonParameters.dueDate-[dueDate] invoice date should be less than or equal to due date;");
                        }
                        if (commonParameters.getDueDate() != null && (request.getBillingType().equals(BillingType.MANUAL_INVOICE) ||
                                                                      request.getBillingType().equals(BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT)) &&
                            (commonParameters.getDueDate().isBefore(LocalDate.now()))) {
                            validationMessageBuilder.append("commonParameters.dueDate-[dueDate] due date cannot be chosen from past;");
                        }
                    }
                    if (commonParameters.getExecutionType() != null) {
                        if (commonParameters.getExecutionType().equals(ExecutionType.EXACT_DATE)) {
                            if (commonParameters.getExecutionDateAndTime() == null) {
                                validationMessageBuilder.append("commonParameters.executionDateAndTime-[executionDateAndTime] is mandatory when ExecutionType is EXACT_DATE;");
                            }
                        } else if (commonParameters.getExecutionType().equals(ExecutionType.IMMEDIATELY) || commonParameters.getExecutionType().equals(ExecutionType.MANUAL)) {
                            if (commonParameters.getExecutionDateAndTime() != null) {
                                validationMessageBuilder.append("commonParameters.executionDateAndTime-[executionDateAndTime] should be empty when ExecutionType is IMMEDIATELY or MANUAL;");
                            }
                        }
                    }
                    if (commonParameters.getExecutionDateAndTime() != null) {
                        if (commonParameters.getExecutionType() != null) {
                            if (commonParameters.getExecutionType().equals(ExecutionType.EXACT_DATE)) {
                                if (commonParameters.getExecutionDateAndTime().isBefore(LocalDateTime.now())) {
                                    validationMessageBuilder.append("commonParameters.executionDateAndTime-[executionDateAndTime] can't be in past;");
                                }
                            }
                        }
                    }
                    checkSendingAnInvoiceAndInvoiceDueDateType(request, commonParameters, validationMessageBuilder);
                } else if (commonParameters.getPeriodicity() != null && Objects.equals(commonParameters.getPeriodicity(), BillingRunPeriodicity.PERIODIC) && !request.getBillingType().equals(BillingType.MANUAL_INVOICE)) {
                    if (CollectionUtils.isEmpty(commonParameters.getProcessPeriodicityIds())) {
                        validationMessageBuilder.append("commonParameters.processPeriodicityIds-[processPeriodicityIds] is mandatory when periodicity is PERIODIC;");
                    }
                    if (commonParameters.getExecutionType() != null) {
                        validationMessageBuilder.append("commonParameters.executionType-[executionType] should be disabled when periodicity is PERIODIC;");
                    }
                    if (commonParameters.getInvoiceDate() != null) {
                        validationMessageBuilder.append("commonParameters.invoiceDate-[invoiceDate] should be disabled when periodicity is PERIODIC;");
                    }
                    if (commonParameters.getTaxEventDate() != null) {
                        validationMessageBuilder.append("commonParameters.taxEventDate-[taxEventDate] should be disabled when periodicity is PERIODIC;");
                    }
                    if (commonParameters.getAccountingPeriodId() != null) {
                        validationMessageBuilder.append("commonParameters.accountingPeriodId-[accountingPeriodId] should be disabled when periodicity is PERIODIC;");
                    }
                    if (!commonParameters.getInvoiceDueDate().equals(InvoiceDueDateType.ACCORDING_TO_THE_CONTRACT)) {
                        validationMessageBuilder.append("commonParameters.invoiceDueDate-[invoiceDueDate] all options,other than ACCORDING_TO_THE_CONTRACT should, be disabled when periodicity is PERIODIC;");
                    }
                    if (commonParameters.getDueDate() != null) {
                        validationMessageBuilder.append("commonParameters.dueDate-[dueDate] dueDate should be null when periodicity is PERIODIC;");
                    }
                    if (commonParameters.getInvoiceDueDate().equals(InvoiceDueDateType.ACCORDING_TO_THE_CONTRACT)) { //FOR STANDARD AND PERIODIC maybe other types should have same validation
                        if (commonParameters.getDueDate() != null) {
                            validationMessageBuilder.append("commonParameters.dueDate-[dueDate] shouldn't be present when billing type is STANDARD_BILLING and InvoiceUdeDate is ACCORDING_TO_THE_CONTRACT;");
                        }
                        if (commonParameters.getInvoiceDate() != null) {
                            validationMessageBuilder.append("commonParameters.invoiceDate-[invoiceDate] should be disabled when periodicity is PERIODIC;");
                        }
                        if (commonParameters.getTaxEventDate() != null) {
                            validationMessageBuilder.append("commonParameters.taxEventDate-[taxEventDate] should be disabled when periodicity is PERIODIC;");
                        }
                        if (commonParameters.getExecutionDateAndTime() != null) {
                            validationMessageBuilder.append("commonParameters.executionDateAndTime-[executionDateAndTime] should be disabled when periodicity is PERIODIC;");
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

        private void checkStandardBillingMaxBilling(BillingRunEditRequest request, BillingRunCommonParameters commonParameters, StringBuilder validationMessageBuilder) {
            StandardBillingParameters basicParameters = request.getBasicParameters();
            if (Objects.equals(commonParameters.getPeriodicity(), BillingRunPeriodicity.STANDARD)) {
                if (basicParameters != null) {
                    if (!CollectionUtils.isEmpty(basicParameters.getApplicationModelType()) &&
                        basicParameters.getApplicationModelType().contains(ApplicationModelType.FOR_VOLUMES)) {
                        if (basicParameters.getMaxEndDate() == null) {
                            validationMessageBuilder.append("basicParameters.maxEndDate-[maxEndDate] Max End Date must be provided when application model type is FOR VOLUMES;");
                        }
                    } else if (basicParameters.getMaxEndDate() != null) {
                        validationMessageBuilder.append("basicParameters.maxEndDate-[maxEndDate] Max End Date must not be provided;");
                    }
                }
            } else if (Objects.equals(commonParameters.getPeriodicity(), BillingRunPeriodicity.PERIODIC)) {
                if (basicParameters != null && basicParameters.getMaxEndDate() != null) {
                    validationMessageBuilder.append("basicParameters.maxEndDate-[maxEndDate] Max End Date must not be provided when periodicity is PERIODIC;");
                }
            }
        }

        private void checkSendingAnInvoiceAndInvoiceDueDateType(BillingRunEditRequest editRequest, BillingRunCommonParameters request, StringBuilder validationMessageBuilder) {
            if (Objects.equals(request.getInvoiceDueDate(), InvoiceDueDateType.ACCORDING_TO_THE_CONTRACT)) {
                if (editRequest.getBillingType().equals(BillingType.MANUAL_INVOICE)) {
                    ManualInvoiceBasicDataParameters basicDataParams = null;
                    ManualInvoiceEditParameters manualInvoiceParameters = editRequest.getManualInvoiceParameters();
                    if (manualInvoiceParameters != null) {
                        basicDataParams = manualInvoiceParameters.getManualInvoiceBasicDataParameters();
                    }
                    boolean hasManualInvoiceParameters = manualInvoiceParameters != null;
                    boolean hasBasicDataParams = basicDataParams != null;
                    boolean isContractOrderIdNull = basicDataParams != null && basicDataParams.getContractOrderId() == null;
                    if (hasManualInvoiceParameters && hasBasicDataParams && isContractOrderIdNull) {
                        validationMessageBuilder.append("commonParameters.invoiceDueDate-[invoiceDueDate] invoice due date must not be 'according to contract' when contract/order is not added;");
                    }
                } else if (editRequest.getBillingType().equals(BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT)) {
                    InterimAndAdvancePaymentParameters interimParameters = editRequest.getInterimAndAdvancePaymentParameters();
                    boolean hasInterimParameters = interimParameters != null;
                    boolean isContractOrderIdNull = interimParameters != null && interimParameters.getContractId() == null;
                    if (hasInterimParameters && isContractOrderIdNull) {
                        validationMessageBuilder.append("commonParameters.invoiceDueDate-[invoiceDueDate] invoice due date must not be 'according to contract' when contract/order is not added;");
                    }
                }
                if (request.getDueDate() != null) {
                    validationMessageBuilder.append("commonParameters.dueDate-[dueDate] should be empty;");
                }
            }
            if (Objects.equals(request.getSendingAnInvoice(), SendingAnInvoice.ACCORDING_TO_THE_CONTRACT)) {
                if (editRequest.getBillingType().equals(BillingType.MANUAL_INVOICE)) {
                    ManualInvoiceBasicDataParameters basicDataParams = null;
                    ManualInvoiceEditParameters manualInvoiceParameters = editRequest.getManualInvoiceParameters();
                    if (manualInvoiceParameters != null) {
                        basicDataParams = manualInvoiceParameters.getManualInvoiceBasicDataParameters();
                    }
                    boolean hasManualInvoiceParameters = manualInvoiceParameters != null;
                    boolean hasBasicDataParams = basicDataParams != null;
                    boolean isContractOrderIdNull = basicDataParams != null && basicDataParams.getContractOrderId() == null;
                    if (hasManualInvoiceParameters && hasBasicDataParams && isContractOrderIdNull) {
                        validationMessageBuilder.append("commonParameters.sendingAnInvoice-[sendingAnInvoice] sending an invoice type must not be 'according to contract' when contract/order is not added;");
                    }
                } else if (editRequest.getBillingType().equals(BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT)) {
                    InterimAndAdvancePaymentParameters interimParameters = editRequest.getInterimAndAdvancePaymentParameters();
                    boolean hasInterimParameters = interimParameters != null;
                    boolean isContractOrderIdNull = interimParameters != null && interimParameters.getContractId() == null;
                    if (hasInterimParameters && isContractOrderIdNull) {
                        validationMessageBuilder.append("commonParameters.sendingAnInvoice-[sendingAnInvoice] sending an invoice type must not be 'according to contract' when contract/order is not added;");
                    }
                }
            }
        }
    }
}
