package bg.energo.phoenix.model.response.contract.productContract;

public record ProductContractAdditionalParamsResponse(
        Long id,
        Long orderingId,
        String label,
        String value
) {
}
