package bg.energo.phoenix.model.response.billing.billingRun.manualInvoice;

import bg.energo.phoenix.model.enums.billing.billings.ManualInvoiceType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ManualInvoiceSummaryDataParametersResponse {

    private ManualInvoiceType manualInvoiceType;

    private List<SummaryDataRowParametersViewResponse> summaryDataRowParametersList;

}
