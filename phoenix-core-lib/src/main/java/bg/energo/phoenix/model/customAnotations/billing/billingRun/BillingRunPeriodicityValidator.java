package bg.energo.phoenix.model.customAnotations.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.*;
import bg.energo.phoenix.model.request.billing.billingRun.BillingRunCommonParameters;
import bg.energo.phoenix.model.request.billing.billingRun.create.BillingRunCreateRequest;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.ManualInvoiceBasicDataParameters;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.ManualInvoiceParameters;
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
@Constraint(validatedBy = BillingRunPeriodicityValidator.BillingRunPeriodicityValidatorImpl.class)
public @interface BillingRunPeriodicityValidator {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class BillingRunPeriodicityValidatorImpl implements ConstraintValidator<BillingRunPeriodicityValidator, BillingRunCreateRequest> {
        @Override
        public boolean isValid(BillingRunCreateRequest createRequest, ConstraintValidatorContext context) {
            boolean isValid = true;
            BillingRunCommonParameters request = createRequest.getCommonParameters();
            StringBuilder validationMessageBuilder = new StringBuilder();

            if (request != null) {
                if (request.getPeriodicity() == null || Objects.equals(request.getPeriodicity(), BillingRunPeriodicity.STANDARD)) {
                    if (request.getAccountingPeriodId() == null) {
                        validationMessageBuilder.append("commonParameters.accountingPeriodId-[accountingPeriodId] should be present when periodicity is STANDARD;");
                    }
                    if (createRequest.getBillingType().equals(BillingType.INVOICE_REVERSAL)) {
                        if (request.getTaxEventDate() != null) {
                            validationMessageBuilder.append("commonParameters.taxEventDate-[taxEventDate] should not be present;");
                        }
                        if (request.getInvoiceDueDate() != null) {
                            validationMessageBuilder.append("commonParameters.invoiceDueDate-[invoiceDueDate] should not be present;");
                        }
                        if (request.getDueDate() != null) {
                            validationMessageBuilder.append("commonParameters.dueDate-[dueDate] should not be present;");
                        }
                    } else {
                        if (request.getTaxEventDate() == null) {
                            validationMessageBuilder.append("commonParameters.taxEventDate-[taxEventDate] should be present when periodicity is STANDARD;");
                        }
                        if (request.getInvoiceDueDate() == null && !(createRequest.getBillingType().equals(BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE) && createRequest.getManualCreditOrDebitNoteParameters().getManualCreditOrDebitNoteBasicDataParameters().getDocumentType().equals(DocumentType.CREDIT_NOTE))) {
                            validationMessageBuilder.append("commonParameters.invoiceDueDate-[invoiceDueDate] is mandatory;");
                        } else if (request.getInvoiceDueDate() != null) {
                            if (createRequest.getBillingType().equals(BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE) && createRequest.getManualCreditOrDebitNoteParameters().getManualCreditOrDebitNoteBasicDataParameters().getDocumentType().equals(DocumentType.CREDIT_NOTE)) {
                                validationMessageBuilder.append("commonParameters.invoiceDueDate-[invoiceDueDate] is should be null;");
                            } else if (request.getInvoiceDueDate().equals(InvoiceDueDateType.DATE)) {
                                if (request.getDueDate() == null) {
                                    validationMessageBuilder.append("commonParameters.dueDate-[dueDate] when invoiceDueDateType is DATE dueDate is mandatory;");
                                }
                                if (request.getInvoiceDate() != null && request.getDueDate() != null && !request.getInvoiceDate().isBefore(request.getDueDate()) && (!request.getInvoiceDate().equals(request.getDueDate()))) {
                                    validationMessageBuilder.append("commonParameters.dueDate-[dueDate] invoice date should be less than or equal to due date;");
                                }
                                if (request.getDueDate() != null && (request.getDueDate().isBefore(LocalDate.now()))) {
                                    validationMessageBuilder.append("commonParameters.dueDate-[dueDate] due date cannot be chosen from past;");
                                }
                            }

                        }
                    }
                    if (request.getInvoiceDate() == null) {
                        validationMessageBuilder.append("commonParameters.invoiceDate-[invoiceDate] should be present when periodicity is STANDARD;");
                    }
                    if (!CollectionUtils.isEmpty(request.getProcessPeriodicityIds())) {
                        validationMessageBuilder.append("commonParameters.processPeriodicityIds-[processPeriodicityIds] should be empty;");
                    }
                    if (request.getExecutionType() == null) {
                        validationMessageBuilder.append("commonParameters.executionType-[executionType] shouldn't be empty when periodicity is STANDARD;");
                    }

                    if (request.getExecutionType() != null) {
                        if (request.getExecutionType().equals(ExecutionType.EXACT_DATE)) {
                            if (request.getExecutionDateAndTime() == null) {
                                validationMessageBuilder.append("commonParameters.executionDateAndTime-[executionDateAndTime] is mandatory when ExecutionType is EXACT_DATE;");
                            }
                        } else if (request.getExecutionType().equals(ExecutionType.IMMEDIATELY) || request.getExecutionType().equals(ExecutionType.MANUAL)) {
                            if (request.getExecutionDateAndTime() != null) {
                                validationMessageBuilder.append("commonParameters.executionDateAndTime-[executionDateAndTime] should be empty when ExecutionType is IMMEDIATELY or MANUAL;");
                            }
                        }
                    }
                    if (request.getExecutionDateAndTime() != null) {
                        if (request.getExecutionType() != null) {
                            if (request.getExecutionType().equals(ExecutionType.EXACT_DATE)) {
                                if (request.getExecutionDateAndTime().isBefore(LocalDateTime.now())) {
                                    validationMessageBuilder.append("commonParameters.executionDateAndTime-[executionDateAndTime] can't be in past;");
                                }
                            }
                        }
                    }
                    checkSendingAnInvoiceAndInvoiceDueDateType(createRequest, request, validationMessageBuilder);

                } else if (Objects.equals(request.getPeriodicity(), BillingRunPeriodicity.PERIODIC) && !createRequest.getBillingType().equals(BillingType.MANUAL_INVOICE)
                           && !createRequest.getBillingType().equals(BillingType.INVOICE_REVERSAL)) {
                    if (CollectionUtils.isEmpty(request.getProcessPeriodicityIds())) {
                        validationMessageBuilder.append("commonParameters.processPeriodicityIds-[processPeriodicityIds] is mandatory when periodicity is PERIODIC;");
                    }
                    if (request.getExecutionType() != null) {
                        validationMessageBuilder.append("commonParameters.executionType-[executionType] should be disabled when periodicity is PERIODIC;");
                    }
                    if (request.getInvoiceDate() != null) {
                        validationMessageBuilder.append("commonParameters.invoiceDate-[invoiceDate] should be disabled when periodicity is PERIODIC;");
                    }
                    if (request.getTaxEventDate() != null) {
                        validationMessageBuilder.append("commonParameters.taxEventDate-[taxEventDate] should be disabled when periodicity is PERIODIC;");
                    }
                    if (request.getAccountingPeriodId() != null) {
                        validationMessageBuilder.append("commonParameters.accountingPeriodId-[accountingPeriodId] should be disabled when periodicity is PERIODIC;");
                    }
                    if (!request.getInvoiceDueDate().equals(InvoiceDueDateType.ACCORDING_TO_THE_CONTRACT)) {
                        validationMessageBuilder.append("commonParameters.invoiceDueDate-[invoiceDueDate] all options,other than ACCORDING_TO_THE_CONTRACT should, be disabled when periodicity is PERIODIC;");
                    }
                    if (request.getDueDate() != null) {
                        validationMessageBuilder.append("commonParameters.dueDate-[dueDate] dueDate should be null when periodicity is PERIODIC;");
                    }
                    if (request.getInvoiceDueDate().equals(InvoiceDueDateType.ACCORDING_TO_THE_CONTRACT)) { //FOR STANDARD AND PERIODIC maybe other types should have same validation
                        if (request.getDueDate() != null) {
                            validationMessageBuilder.append("commonParameters.dueDate-[dueDate] shouldn't be present when billing type is STANDARD_BILLING and InvoiceUdeDate is ACCORDING_TO_THE_CONTRACT;");
                        }
                        if (request.getInvoiceDate() != null) {
                            validationMessageBuilder.append("commonParameters.invoiceDate-[invoiceDate] should be disabled when periodicity is PERIODIC;");
                        }
                        if (request.getTaxEventDate() != null) {
                            validationMessageBuilder.append("commonParameters.taxEventDate-[taxEventDate] should be disabled when periodicity is PERIODIC;");
                        }
                        if (request.getExecutionDateAndTime() != null) {
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

        private void checkSendingAnInvoiceAndInvoiceDueDateType(BillingRunCreateRequest createRequest, BillingRunCommonParameters request, StringBuilder validationMessageBuilder) {
            if (Objects.equals(request.getInvoiceDueDate(), InvoiceDueDateType.ACCORDING_TO_THE_CONTRACT)) {
                if (createRequest.getBillingType().equals(BillingType.MANUAL_INVOICE)) {
                    ManualInvoiceBasicDataParameters basicDataParams = null;
                    ManualInvoiceParameters manualInvoiceParameters = createRequest.getManualInvoiceParameters();
                    if (manualInvoiceParameters != null) {
                        basicDataParams = manualInvoiceParameters.getManualInvoiceBasicDataParameters();
                    }
                    boolean hasManualInvoiceParameters = manualInvoiceParameters != null;
                    boolean hasBasicDataParams = basicDataParams != null;
                    boolean isContractOrderIdNull = basicDataParams != null && basicDataParams.getContractOrderId() == null;
                    if (hasManualInvoiceParameters && hasBasicDataParams && isContractOrderIdNull) {
                        validationMessageBuilder.append("commonParameters.invoiceDueDate-[invoiceDueDate] invoice due date must not be 'according to contract' when contract/order is not added;");
                    }
                } else if (createRequest.getBillingType().equals(BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT)) {
                    InterimAndAdvancePaymentParameters interimParameters = createRequest.getInterimAndAdvancePaymentParameters();
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
                if (createRequest.getBillingType().equals(BillingType.MANUAL_INVOICE)) {
                    ManualInvoiceBasicDataParameters basicDataParams = null;
                    ManualInvoiceParameters manualInvoiceParameters = createRequest.getManualInvoiceParameters();
                    if (manualInvoiceParameters != null) {
                        basicDataParams = manualInvoiceParameters.getManualInvoiceBasicDataParameters();
                    }
                    boolean hasManualInvoiceParameters = manualInvoiceParameters != null;
                    boolean hasBasicDataParams = basicDataParams != null;
                    boolean isContractOrderIdNull = basicDataParams != null && basicDataParams.getContractOrderId() == null;
                    if (hasManualInvoiceParameters && hasBasicDataParams && isContractOrderIdNull) {
                        validationMessageBuilder.append("commonParameters.sendingAnInvoice-[sendingAnInvoice] sending an invoice type must not be 'according to contract' when contract/order is not added;");
                    }
                } else if (createRequest.getBillingType().equals(BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT)) {
                    InterimAndAdvancePaymentParameters interimParameters = createRequest.getInterimAndAdvancePaymentParameters();
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
