package bg.energo.phoenix.model.response.crm.emailCommunication;

public record CustomerShortResponse(
        String customerIdentifier,
        Long versionId
) {
}
