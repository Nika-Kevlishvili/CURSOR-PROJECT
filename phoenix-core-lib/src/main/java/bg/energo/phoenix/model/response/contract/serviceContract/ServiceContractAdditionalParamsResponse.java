package bg.energo.phoenix.model.response.contract.serviceContract;

public record ServiceContractAdditionalParamsResponse(
        Long id,
        Long orderingId,
        String label,
        String value
) {
}
