package bg.energo.phoenix.model.customAnotations.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.*;
import bg.energo.phoenix.model.request.billing.billingRun.BillingRunCommonParameters;
import bg.energo.phoenix.model.request.billing.billingRun.StandardBillingParameters;
import bg.energo.phoenix.model.request.billing.billingRun.create.BillingRunCreateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDateTime;
import java.util.Objects;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = BillingRunBaseRequestValidator.BillingRunBaseRequestValidatorImpl.class)
public @interface BillingRunBaseRequestValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class BillingRunBaseRequestValidatorImpl implements ConstraintValidator<BillingRunBaseRequestValidator, BillingRunCreateRequest> {
        @Override
        public boolean isValid(BillingRunCreateRequest request, ConstraintValidatorContext context) {
            boolean isValid = true;
            StringBuilder validationMessageBuilder = new StringBuilder();
            BillingRunCommonParameters commonParameters = request.getCommonParameters();
            if (commonParameters != null) {
                if (Objects.equals(request.getBillingType(), BillingType.MANUAL_INVOICE)) {
                    if (Objects.equals(commonParameters.getPeriodicity(), BillingRunPeriodicity.PERIODIC)) {
                        validationMessageBuilder.append("commonParameters.periodicity-[periodicity] should be STANDARD for manual invoice;");
                    }
                }

                if (Objects.equals(request.getBillingType(), BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE)) {
                    if (Objects.equals(commonParameters.getPeriodicity(), BillingRunPeriodicity.PERIODIC)) {
                        validationMessageBuilder.append("commonParameters.periodicity-[periodicity] should be STANDARD for manual credit or debit note;");
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

                if (Objects.equals(request.getBillingType(), BillingType.STANDARD_BILLING)) {
                    StandardBillingParameters basicParameters = request.getBasicParameters();
                    if (Objects.equals(commonParameters.getPeriodicity(), BillingRunPeriodicity.STANDARD)) {
                        if (basicParameters != null) {
                            if (CollectionUtils.isNotEmpty(basicParameters.getApplicationModelType()) &&
                                basicParameters.getApplicationModelType().contains(ApplicationModelType.FOR_VOLUMES)) {
                                if (basicParameters.getMaxEndDate() == null) {
                                    validationMessageBuilder.append("basicParameters.maxEndDate-[maxEndDate] Max End Date must be provided when application model type is 'For Volumes';");
                                }
                            } else if (basicParameters.getMaxEndDate() != null) {
                                validationMessageBuilder.append("basicParameters.maxEndDate-[maxEndDate] Max End Date must not be provided;");
                            }
                            if (basicParameters.getPeriodicMaxEndDate() != null) {
                                validationMessageBuilder.append("basicParameters.periodicMaxEndDate-[periodicMaxEndDate] Periodic max end date must not be provided;");
                            }
                            if (basicParameters.getPeriodicMaxEndDateValue() != null) {
                                validationMessageBuilder.append("basicParameters.periodicMaxEndDateValue-[periodicMaxEndDateValue] Periodic max end date value must not be provided;");
                            }
                        }
                        if (commonParameters.getAccountingPeriodId() == null) {
                            validationMessageBuilder.append("commonParameters.accountingPeriodId-[accountingPeriodId] should be present when billing type is STANDARD_BILLING;");
                        }
                        if (commonParameters.getTaxEventDate() == null) {
                            validationMessageBuilder.append("commonParameters.taxEventDate-[taxEventDate] should be present when billing type is STANDARD_BILLING;");
                        }
                        if (commonParameters.getInvoiceDate() == null) {
                            validationMessageBuilder.append("commonParameters.invoiceDate-[invoiceDate] should be present when billing type is STANDARD_BILLING;");
                        }
                    } else if (Objects.equals(commonParameters.getPeriodicity(), BillingRunPeriodicity.PERIODIC)) {
                        if (basicParameters != null) {
                            if (basicParameters.getMaxEndDate() != null) {
                                validationMessageBuilder.append("basicParameters.maxEndDate-[maxEndDate] Max End Date must not be provided when periodicity is 'Periodic';");
                            }

                            if (CollectionUtils.isNotEmpty(basicParameters.getApplicationModelType()) &&
                                basicParameters.getApplicationModelType().contains(ApplicationModelType.FOR_VOLUMES)) {
                                if (basicParameters.getPeriodicMaxEndDate() == null) {
                                    validationMessageBuilder.append("basicParameters.periodicMaxEndDate-[periodicMaxEndDate] Periodic max end date must be provided when application model type is 'For Volumes';");
                                }
                                if (basicParameters.getPeriodicMaxEndDateValue() == null) {
                                    validationMessageBuilder.append("basicParameters.periodicMaxEndDateValue-[periodicMaxEndDateValue] Periodic max end date value must be provided when application model type is 'For Volumes';");
                                }
                            } else {
                                if (basicParameters.getPeriodicMaxEndDate() != null) {
                                    validationMessageBuilder.append("basicParameters.periodicMaxEndDate-[periodicMaxEndDate] Periodic max end date must not be provided;");
                                }
                                if (basicParameters.getPeriodicMaxEndDateValue() != null) {
                                    validationMessageBuilder.append("basicParameters.periodicMaxEndDateValue-[periodicMaxEndDateValue] Periodic max end date value must not be provided;");
                                }
                            }
                        }

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
                    if (invoiceDueDate != null && invoiceDueDate.equals(InvoiceDueDateType.DATE) && commonParameters.getDueDate() == null) {
                        validationMessageBuilder.append("dueDate-dueDate is mandatory when Due date type is DATE;");
                    } else if (invoiceDueDate != null && invoiceDueDate.equals(InvoiceDueDateType.ACCORDING_TO_THE_CONTRACT) && commonParameters.getDueDate() != null) {
                        validationMessageBuilder.append("dueDate-dueDate must be null when Due date type is According to the Contract;");
                    }

                    if (commonParameters.getDueDate() != null && commonParameters.getInvoiceDate() != null && commonParameters.getDueDate().isBefore(commonParameters.getInvoiceDate())) {
                        validationMessageBuilder.append("dueDate-dueDate can not be before invoice Date;");
                    }
                }

                if (!validationMessageBuilder.isEmpty()) {
                    context.buildConstraintViolationWithTemplate(validationMessageBuilder.toString()).addConstraintViolation();
                    isValid = false;
                }


            }
            return isValid;
        }


    }
}
