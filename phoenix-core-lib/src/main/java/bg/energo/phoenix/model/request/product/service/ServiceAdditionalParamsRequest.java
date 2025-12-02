package bg.energo.phoenix.model.request.product.service;

public record ServiceAdditionalParamsRequest(
        Long orderingId,
        String label,
        String value
) {
}
