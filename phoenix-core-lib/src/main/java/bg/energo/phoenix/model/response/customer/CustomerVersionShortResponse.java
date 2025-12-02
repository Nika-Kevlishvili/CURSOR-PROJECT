package bg.energo.phoenix.model.response.customer;

public record CustomerVersionShortResponse(
        Long id,
        String name,
        Long versionId
) {
}
