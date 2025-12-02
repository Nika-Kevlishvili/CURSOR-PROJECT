package bg.energo.phoenix.model.response.billing.invoice;

import bg.energo.phoenix.model.response.billing.billingRun.manualCreditOrDebitNote.BillingRunInvoiceResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvailableInvoiceResponse {
    private boolean canSelectAccordingToContract;
    private Page<BillingRunInvoiceResponse> availableInvoices;
}
