package bg.energo.phoenix.model.request.communication.edms;

public record UploadFileResponse(
        String description,
        Long documentId,
        Long fileId
) {
}
