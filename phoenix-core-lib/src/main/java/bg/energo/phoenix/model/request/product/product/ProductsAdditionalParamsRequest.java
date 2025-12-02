package bg.energo.phoenix.model.request.product.product;

public record ProductsAdditionalParamsRequest(
        Long orderingId,
        String label,
        String value
) {
}
