package bg.energo.phoenix.model.response.nomenclature.product;

public record ProductAdditionalParamsResponse(
        Long orderingId,
        Long productDetailId,
        String label,
        String value
) {
}
