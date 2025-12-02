package bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice;

import bg.energo.phoenix.model.customAnotations.billing.billingRun.ManualInvoiceParametersValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@ManualInvoiceParametersValidator
public class ManualInvoiceParameters {

    @NotNull(message = "manualInvoiceBasicDataParameters should not be null;")
    private @Valid ManualInvoiceBasicDataParameters manualInvoiceBasicDataParameters;

    @NotNull(message = "manualInvoiceSummaryDataParameters should not be null;")
    private @Valid ManualInvoiceSummaryDataParameters manualInvoiceSummaryDataParameters;

    private @Valid ManualInvoiceDetailedDataParameters manualInvoiceDetailedDataParameters;
}
