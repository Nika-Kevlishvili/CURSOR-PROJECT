package bg.energo.phoenix.model.response.receivable.disconnectionPowerSupply;

public record PodAndGotMappedResponse(
        Long podId,
        String podIdentifier,
        Long gotId,
        String disconnectionType
) {
}
