package bg.energo.phoenix.model.request.billing.billingRun;

import jakarta.validation.constraints.NotNull;

public record BillingRunInvoiceListingRequest(
        @NotNull(message = "page-page must not be null;")
        Integer page,
        @NotNull(message = "size-size must not be null;")
        Integer size,
        String prompt
) {
}
