package bg.energo.phoenix.model.response.billing.billingRun.manualInvoice;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ManualInvoiceBillingRunParametersResponse {

    private ManualInvoiceBasicDataParametersResponse manualInvoiceBasicDataParameters;

    private ManualInvoiceSummaryDataParametersResponse manualInvoiceSummaryDataParameters;

    private ManualInvoiceDetailedDataParametersResponse manualInvoiceDetailedDataParameters;
}
