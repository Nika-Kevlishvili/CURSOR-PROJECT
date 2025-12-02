package bg.energo.phoenix.model.response.billing.billingRun;

import bg.energo.phoenix.model.response.billing.billingRun.manualCreditOrDebitNote.ManualCreditOrDebitNoteBillingRunParametersResponse;
import bg.energo.phoenix.model.response.billing.billingRun.manualInvoice.ManualInvoiceBillingRunParametersResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BillingRunResponse {

    private BillingRunCommonParametersResponse commonParameters;

    private StandardBillingRunParametersResponse standardBillingRunParameters;

    private InvoiceCorrectionBillingRunParametersResponse invoiceCorrectionBillingRunParametersResponse;

    private ManualInvoiceBillingRunParametersResponse manualInvoiceBillingRunParameters;

    private ManualCreditOrDebitNoteBillingRunParametersResponse manualCreditOrDebitNoteBillingRunParametersResponse;

    private ManualInterimAndAdvancePaymentParametersResponse manualInterimAndAdvancePaymentParametersResponse;

    private InvoiceReversalBillingRunParametersResponse invoiceReversalBillingRunParametersResponse;

    private List<BillingRunTasksResponse> billingRunTasks;

    private String employeeId;

}
