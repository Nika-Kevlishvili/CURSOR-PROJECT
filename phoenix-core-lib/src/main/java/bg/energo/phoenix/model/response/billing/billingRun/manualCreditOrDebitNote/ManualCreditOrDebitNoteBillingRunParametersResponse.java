package bg.energo.phoenix.model.response.billing.billingRun.manualCreditOrDebitNote;

import bg.energo.phoenix.model.response.billing.billingRun.manualInvoice.ManualInvoiceDetailedDataParametersResponse;
import bg.energo.phoenix.model.response.billing.billingRun.manualInvoice.ManualInvoiceSummaryDataParametersResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualCreditOrDebitNoteBillingRunParametersResponse {

    private ManualCreditOrDebitNoteBasicDataParametersResponse manualCreditOrDebitNoteBasicDataParameters;

    private ManualInvoiceSummaryDataParametersResponse manualCreditOrDebitNoteSummaryDataParameters;

    private ManualInvoiceDetailedDataParametersResponse manualCreditOrDebitNoteDetailedDataParameters;
}
