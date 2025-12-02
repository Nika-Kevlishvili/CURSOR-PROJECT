package bg.energo.phoenix.model.response.template;

import bg.energo.phoenix.model.enums.template.ContractTemplateLanguage;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;

import java.time.LocalDateTime;

public interface TemplateListingMiddleResponse {
    Long getId();
    String getName();
    ContractTemplateType getType();
    ContractTemplatePurposes getPurpose();
    String getFileSignings();
    String getOutputFileFormats();
    ContractTemplateLanguage getLanguage();
    LocalDateTime getCreateDate();
    ContractTemplateStatus getStatus();
}
