package bg.energo.phoenix.model.request.billing.billingRun.edit;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.customAnotations.billing.billingRun.BillingRunEditBaseRequestValidator;
import bg.energo.phoenix.model.customAnotations.billing.billingRun.BillingRunEditTypeValidator;
import bg.energo.phoenix.model.enums.billing.billings.BillingType;
import bg.energo.phoenix.model.request.billing.billingRun.BillingRunCommonParameters;
import bg.energo.phoenix.model.request.billing.billingRun.InvoiceCorrectionParameters;
import bg.energo.phoenix.model.request.billing.billingRun.StandardBillingParameters;
import bg.energo.phoenix.model.request.billing.billingRun.create.InvoiceReversalParameters;
import bg.energo.phoenix.model.request.billing.billingRun.edit.manualInvoice.ManualInvoiceEditParameters;
import bg.energo.phoenix.model.request.billing.billingRun.iap.InterimAndAdvancePaymentParameters;
import bg.energo.phoenix.model.request.billing.billingRun.manualCreditOrDebitNote.ManualCreditOrDebitNoteParameters;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@BillingRunEditTypeValidator
@BillingRunEditBaseRequestValidator
public class BillingRunEditRequest {

    public @Valid StandardBillingParameters basicParameters;

    private @Valid BillingRunCommonParameters commonParameters;

    private @Valid InterimAndAdvancePaymentParameters interimAndAdvancePaymentParameters;

    private @Valid ManualInvoiceEditParameters manualInvoiceParameters;

    private @Valid InvoiceCorrectionParameters InvoiceCorrectionParameters;

    private @Valid InvoiceReversalParameters invoiceReversalParameters;

    private @Valid ManualCreditOrDebitNoteParameters manualCreditOrDebitNoteParameters;

    @NotNull(message = "basicParameters.billingType-[billingType] must not be null;")
    private BillingType billingType;

    @DuplicatedValuesValidator(message = "taskId")
    public List<Long> taskId;

    @JsonIgnore
    @AssertTrue(message = "commonParameters.periodicity-[periodicity] must not be null;")
    public boolean isPeriodicityValid() {
        if (billingType != null) {
            switch (billingType) {
                case STANDARD_BILLING, MANUAL_INVOICE, MANUAL_CREDIT_OR_DEBIT_NOTE, MANUAL_INTERIM_AND_ADVANCE_PAYMENT -> {
                    if (commonParameters.getPeriodicity() == null) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
