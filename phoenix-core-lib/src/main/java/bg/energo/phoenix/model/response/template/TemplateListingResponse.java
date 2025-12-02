package bg.energo.phoenix.model.response.template;

import bg.energo.phoenix.model.enums.template.*;
import bg.energo.phoenix.util.epb.EPBListUtils;

import java.time.LocalDate;
import java.util.List;

public record TemplateListingResponse(
        Long id,
        String name,
        ContractTemplateType type,
        ContractTemplatePurposes purpose,
        List<ContractTemplateSigning> fileSignings,
        List<ContractTemplateFileFormat> outputFileFormats,

        ContractTemplateLanguage language,
        LocalDate creationDate,
        ContractTemplateStatus status
) {
    public TemplateListingResponse(TemplateListingMiddleResponse response) {
        this(response.getId(),
                response.getName(),
                response.getType(),
                response.getPurpose(),
                EPBListUtils.convertDBEnumStringArrayIntoListEnum(ContractTemplateSigning.class, response.getFileSignings()),
                EPBListUtils.convertDBEnumStringArrayIntoListEnum(ContractTemplateFileFormat.class, response.getOutputFileFormats()),
                response.getLanguage(),
                response.getCreateDate().toLocalDate(),
                response.getStatus()
        );
    }
}
