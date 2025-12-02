package bg.energo.phoenix.model.request.billing.billingRun.edit.manualInvoice;

import bg.energo.phoenix.model.customAnotations.billing.billingRun.SummaryDataRowIdsValidator;
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
@SummaryDataRowIdsValidator
public class ManualInvoiceSummaryDataEditParameters {
    @NotNull(message = "manualInvoiceSummaryDataEditParameters.manualInvoiceType-[manualInvoiceSummaryDataType] must not be null;")
    private ManualInvoiceType manualInvoiceType;

    private List<@Valid SummaryDataRowEditParameters> summaryDataRowList;

}
