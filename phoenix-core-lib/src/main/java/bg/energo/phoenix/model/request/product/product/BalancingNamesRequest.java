package bg.energo.phoenix.model.request.product.product;

import jakarta.validation.constraints.NotNull;

public record BalancingNamesRequest(
        String prompt,
        @NotNull(message = "page-Page must not be null")
        Integer page,
        @NotNull(message = "size-Size must not be null")
        Integer size
) {
}
