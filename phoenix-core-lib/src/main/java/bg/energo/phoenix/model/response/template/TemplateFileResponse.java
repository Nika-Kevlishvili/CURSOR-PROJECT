package bg.energo.phoenix.model.response.template;

import bg.energo.phoenix.model.entity.template.ContractTemplateFiles;
import bg.energo.phoenix.util.epb.EPBFinalFields;

public record TemplateFileResponse(Long id, String name) {
    public TemplateFileResponse(ContractTemplateFiles file) {
        this(
                file.getId(),
                file.getName().length() > EPBFinalFields.UUID_PREFIX_LENGTH_IN_FILE_NAME ?
                        file.getName().substring(EPBFinalFields.UUID_PREFIX_LENGTH_IN_FILE_NAME) :
                        file.getName()
        );
    }
}
