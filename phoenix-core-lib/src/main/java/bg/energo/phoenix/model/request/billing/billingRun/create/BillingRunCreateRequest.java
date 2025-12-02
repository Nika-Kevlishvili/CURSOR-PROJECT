package bg.energo.phoenix.model.request.billing.billingRun.create;

import bg.energo.phoenix.model.customAnotations.billing.billingRun.BillingRunBaseRequestValidator;
import bg.energo.phoenix.model.customAnotations.billing.billingRun.BillingRunPeriodicityValidator;
import bg.energo.phoenix.model.customAnotations.billing.billingRun.BillingRunTemplateValidator;
import bg.energo.phoenix.model.customAnotations.billing.billingRun.BillingRunTypeValidator;
import bg.energo.phoenix.model.enums.billing.billings.BillingType;
import bg.energo.phoenix.model.request.billing.billingRun.BillingRunCommonParameters;
import bg.energo.phoenix.model.request.billing.billingRun.InvoiceCorrectionParameters;
import bg.energo.phoenix.model.request.billing.billingRun.StandardBillingParameters;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.ManualInvoiceParameters;
import bg.energo.phoenix.model.request.billing.billingRun.iap.InterimAndAdvancePaymentParameters;
import bg.energo.phoenix.model.request.billing.billingRun.manualCreditOrDebitNote.ManualCreditOrDebitNoteParameters;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@BillingRunBaseRequestValidator
@BillingRunTypeValidator
@BillingRunPeriodicityValidator
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@BillingRunTemplateValidator
public class BillingRunCreateRequest {

    private @Valid StandardBillingParameters basicParameters;

    private @Valid BillingRunCommonParameters commonParameters;

    private @Valid InterimAndAdvancePaymentParameters interimAndAdvancePaymentParameters;

    private @Valid ManualInvoiceParameters manualInvoiceParameters;

    private @Valid InvoiceCorrectionParameters invoiceCorrectionParameters;

    private @Valid ManualCreditOrDebitNoteParameters manualCreditOrDebitNoteParameters;

    private @Valid InvoiceReversalParameters invoiceReversalParameters;

    private Long taskId;

    @NotNull(message = "billingRunCreateRequest.billingType-[billingType] must not be null;")
    private BillingType billingType;

    @JsonIgnore
    @AssertTrue(message = "commonParameters.periodicity-[periodicity] must not be null;")
    public boolean isPeriodicityValid() {
        if (billingType != null) {
            switch (billingType) {
                case STANDARD_BILLING,
                     MANUAL_INVOICE,
                     MANUAL_CREDIT_OR_DEBIT_NOTE,
                     MANUAL_INTERIM_AND_ADVANCE_PAYMENT -> {
                    if (commonParameters.getPeriodicity() == null) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
