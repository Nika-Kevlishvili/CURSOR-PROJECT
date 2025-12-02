package bg.energo.phoenix.model.response.contract;

public interface DocumentGenerationPopupMiddleResponse {
    Long getTemplateId();

    Integer getTemplateVersion();

    String getTemplateName();

    String getOutputFileFormat();

    String getFileSignings();
}
