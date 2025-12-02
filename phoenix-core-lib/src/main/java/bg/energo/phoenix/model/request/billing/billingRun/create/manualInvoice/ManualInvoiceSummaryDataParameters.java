package bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice;

import bg.energo.phoenix.model.enums.billing.billings.ManualInvoiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ManualInvoiceSummaryDataParameters {

    @NotNull(message = "manualInvoiceSummaryDataParameters.manualInvoiceType-[manualInvoiceSummaryDataType] must not be null;")
    private ManualInvoiceType manualInvoiceType;

    private List<@Valid SummaryDataRowParameters> summaryDataRowList;

}
