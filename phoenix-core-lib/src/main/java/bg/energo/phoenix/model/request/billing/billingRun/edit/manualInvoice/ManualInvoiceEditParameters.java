package bg.energo.phoenix.model.request.billing.billingRun.edit.manualInvoice;

import bg.energo.phoenix.model.customAnotations.billing.billingRun.ManualInvoiceEditParametersValidator;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.ManualInvoiceBasicDataParameters;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ManualInvoiceEditParametersValidator
public class ManualInvoiceEditParameters {

    @NotNull(message = "manualInvoiceBasicDataParameters should not be null;")
    private @Valid ManualInvoiceBasicDataParameters manualInvoiceBasicDataParameters;

    @NotNull(message = "manualInvoiceSummaryDataParameters should not be null;")
    private @Valid ManualInvoiceSummaryDataEditParameters manualInvoiceSummaryDataParameters;

    private @Valid ManualInvoiceDetailedDataEditParameters manualInvoiceDetailedDataParameters;

}
