package bg.energo.phoenix.model.request.billing.invoice;

import jakarta.validation.constraints.NotNull;

public record InvoiceDataListingRequest(
        @NotNull
        Integer page,

        @NotNull
        Integer size
) {
}
