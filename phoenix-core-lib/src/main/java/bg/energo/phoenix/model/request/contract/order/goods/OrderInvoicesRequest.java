package bg.energo.phoenix.model.request.contract.order.goods;

import jakarta.validation.constraints.NotNull;

public record OrderInvoicesRequest(
        @NotNull(message = "page-page must not be null;")
        Integer page,
        @NotNull(message = "size-size must not be null;")
        Integer size,
        String prompt
) {
}
