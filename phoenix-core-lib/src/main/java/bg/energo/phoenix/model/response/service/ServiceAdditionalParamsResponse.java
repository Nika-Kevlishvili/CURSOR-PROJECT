package bg.energo.phoenix.model.response.service;

public record ServiceAdditionalParamsResponse(
        Long orderingId,
        Long serviceDetailId,
        String label,
        String value
) {
}
