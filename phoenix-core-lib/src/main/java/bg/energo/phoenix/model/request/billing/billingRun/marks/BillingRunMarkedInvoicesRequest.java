package bg.energo.phoenix.model.request.billing.billingRun.marks;

import java.util.List;

public record BillingRunMarkedInvoicesRequest(
        List<Long> invoiceIds,
        List<Long> excludedInvoiceIds
) {
}
