package bg.energo.phoenix.model.request.billing.billingRun.manualCreditOrDebitNote;

import bg.energo.phoenix.model.customAnotations.billing.billingRun.ManualCreditOrDebitNoteParametersValidator;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.ManualInvoiceDetailedDataParameters;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.ManualInvoiceSummaryDataParameters;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ManualCreditOrDebitNoteParametersValidator
public class ManualCreditOrDebitNoteParameters {
    @NotNull(message = "manualCreditOrDebitNoteBasicDataParameters should not be null;")
    private @Valid ManualCreditOrDebitNoteBasicDataParameters manualCreditOrDebitNoteBasicDataParameters;

    @NotNull(message = "manualCreditOrDebitNoteSummaryDataParameters should not be null;")
    private @Valid ManualInvoiceSummaryDataParameters manualCreditOrDebitNoteSummaryDataParameters;

    private @Valid ManualInvoiceDetailedDataParameters manualCreditOrDebitNoteDetailedDataParameters;
}
